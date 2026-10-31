package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.LootBoxSource;
import com.successhub.appdomain.enums.LootBoxOpenStatus;
import com.successhub.appdomain.enums.RewardType;
import com.successhub.appdomain.enums.RewardRarity;
import com.successhub.appdomain.dto.gamification.BoxTypeDto;
import com.successhub.appdomain.dto.gamification.InventoryItemDto;
import com.successhub.appdomain.dto.gamification.LootBoxDto;
import com.successhub.appdomain.dto.gamification.RewardItemDto;
import com.successhub.appdomain.entity.LootBoxContent;
import com.successhub.appdomain.enums.LootBoxType;
import com.successhub.appdomain.entity.RewardDefinition;
import com.successhub.appdomain.entity.UserInventory;
import com.successhub.appdomain.entity.UserLootBox;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.LootBoxContentRepository;
import com.successhub.appdomain.repository.RewardDefinitionRepository;
import com.successhub.appdomain.repository.StreakDayOverrideRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserInventoryRepository;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.repository.UserLootBoxRepository;
import com.successhub.appdomain.service.LootBoxService;
import com.successhub.appdomain.service.gamification.GamificationTimeUtil;
import com.successhub.appdomain.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LootBoxServiceImpl implements LootBoxService {

    /** Each box opens as exactly 3 independent rolls in V1 (no guaranteed slot, no pity timer). */
    private static final int SLOTS_PER_BOX = 3;

    private final UserLootBoxRepository lootBoxRepository;
    private final LootBoxContentRepository contentRepository;
    private final RewardDefinitionRepository rewardDefinitionRepository;
    private final UserInventoryRepository inventoryRepository;
    private final TaskRepository taskRepository;
    private final StreakDayOverrideRepository overrideRepository;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UUID grantLootBox(String userId, LootBoxSource source) {
        return createBox(userId, source, defaultBoxType(source)).getId();
    }

    @Override
    @Transactional
    public LootBoxDto grantBox(String userId, LootBoxType boxType) {
        UserLootBox box = createBox(userId, LootBoxSource.MANUAL, boxType);
        return toDto(box, List.of());
    }

    @Override
    public List<BoxTypeDto> getBoxTypes() {
        return Arrays.stream(LootBoxType.values())
                .map(t -> new BoxTypeDto(
                        t.name(), t.getDisplayName(), t.getSource(), t.getFeel(), t.getBlurb(), t.getIcon(),
                        t.getCommonWeight(), t.getRareWeight(), t.getLegendaryWeight()))
                .toList();
    }

    @Override
    @Transactional
    public LootBoxDto openLootBox(String userId, UUID lootBoxId) {
        UserLootBox box = lootBoxRepository.findByIdAndUserId(lootBoxId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Loot box not found"));
        if (box.getStatus() == LootBoxOpenStatus.OPENED) {
            return toDto(box, contentRepository.findByLootBox_Id(box.getId()).stream()
                    .map(LootBoxContent::getRewardDefinition)
                    .map(this::toRewardItem)
                    .toList());
        }

        Map<RewardRarity, List<RewardDefinition>> poolByRarity = poolsByRarity();
        List<RewardItemDto> rolled = new ArrayList<>();
        for (int slot = 0; slot < SLOTS_PER_BOX; slot++) {
            RewardDefinition reward = roll(box.getBoxType(), poolByRarity);
            LootBoxContent content = new LootBoxContent();
            content.setLootBox(box);
            content.setRewardDefinition(reward);
            contentRepository.save(content);
            addToInventory(userId, reward);
            rolled.add(toRewardItem(reward));
        }

        box.setStatus(LootBoxOpenStatus.OPENED);
        box.setOpenedAt(Instant.now());
        lootBoxRepository.save(box);
        return toDto(box, rolled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LootBoxDto> getPendingLootBoxes(String userId) {
        return lootBoxRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, LootBoxOpenStatus.PENDING).stream()
                .map(b -> toDto(b, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LootBoxDto> getLootBoxHistory(String userId, int limit) {
        int capped = Math.max(1, Math.min(limit, 10));
        return lootBoxRepository
                .findByUserIdAndStatusOrderByOpenedAtDesc(
                        userId, LootBoxOpenStatus.OPENED, PageRequest.of(0, capped))
                .stream()
                .map(box -> toDto(box, contentRepository.findByLootBox_Id(box.getId()).stream()
                        .map(LootBoxContent::getRewardDefinition)
                        .map(this::toRewardItem)
                        .toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemDto> getInventory(String userId) {
        return inventoryRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(i -> new InventoryItemDto(i.getId(), toRewardItem(i.getRewardDefinition()), i.getQuantity(), i.isEquipped()))
                .toList();
    }

    @Override
    @Transactional
    public InventoryItemDto toggleEquip(String userId, UUID inventoryId) {
        UserInventory item = inventoryRepository.findById(inventoryId)
                .filter(i -> i.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));

        RewardType type = item.getRewardDefinition().getType();
        if (type != RewardType.TITLE && type != RewardType.FRAME) {
            // Functional items are not equippable; return unchanged.
            return new InventoryItemDto(item.getId(), toRewardItem(item.getRewardDefinition()), item.getQuantity(), item.isEquipped());
        }

        boolean willEquip = !item.isEquipped();
        if (willEquip) {
            for (UserInventory other : inventoryRepository.findByUserIdOrderByIdAsc(userId)) {
                if (other.getRewardDefinition().getType() == type && other.isEquipped() && !other.getId().equals(item.getId())) {
                    other.setEquipped(false);
                    inventoryRepository.save(other);
                }
            }
        }
        item.setEquipped(willEquip);
        inventoryRepository.save(item);
        return new InventoryItemDto(item.getId(), toRewardItem(item.getRewardDefinition()), item.getQuantity(), item.isEquipped());
    }

    @Override
    @Transactional
    public UUID checkStreakMilestone(String userId, int streak) {
        if (streak == 7 || streak == 30) {
            return createBox(userId, LootBoxSource.STREAK_MILESTONE, LootBoxType.IRON_CHEST).getId();
        }
        if (streak == 100) {
            return createBox(userId, LootBoxSource.STREAK_MILESTONE, LootBoxType.SOVEREIGN_VAULT).getId();
        }
        return null;
    }

    @Override
    @Transactional
    public UUID checkWeeklyLoot(String userId, UserProfile profile, LocalDate weekMonday) {
        if (weekMonday.equals(profile.getLastWeeklyLootWeek())) {
            return null;
        }
        int qualifying = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekMonday.plusDays(i);
            if (isQualifyingDay(userId, day)) {
                qualifying++;
            }
        }
        if (qualifying < properties.getWeeklyLootMinQualifyingDays()) {
            return null;
        }
        profile.setLastWeeklyLootWeek(weekMonday);
        return createBox(userId, LootBoxSource.WEEKLY_RESET, LootBoxType.SOVEREIGN_VAULT).getId();
    }

    private boolean isQualifyingDay(String userId, LocalDate day) {
        if (overrideRepository.existsByUserIdAndDay(userId, day)) {
            return true;
        }
        long count = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(day), timeUtil.endOfDay(day));
        return count >= properties.getMinTasksForQualifyingDay();
    }

    private UserLootBox createBox(String userId, LootBoxSource source, LootBoxType boxType) {
        UserLootBox box = new UserLootBox();
        box.setUserId(userId);
        box.setSource(source);
        box.setBoxType(boxType);
        box.setStatus(LootBoxOpenStatus.PENDING);
        UserLootBox saved = lootBoxRepository.save(box);
        publishLootNotification(userId, source, boxType, saved.getId());
        return saved;
    }

    private void publishLootNotification(String userId, LootBoxSource source, LootBoxType boxType, UUID boxId) {
        if (source == LootBoxSource.STREAK_MILESTONE) {
            eventPublisher.publishEvent(new GameNotificationEvent(
                    userId,
                    NotificationType.STREAK_MILESTONE,
                    "Streak milestone",
                    "You earned a " + boxType.getDisplayName() + " for your streak.",
                    "/loot-boxes",
                    boxId));
            return;
        }
        String reason = switch (source) {
            case ACHIEVEMENT -> "an achievement unlock";
            case WEEKLY_RESET -> "your weekly ritual";
            case MANUAL -> "a grant";
            default -> "your progress";
        };
        eventPublisher.publishEvent(new GameNotificationEvent(
                userId,
                NotificationType.LOOT_EARNED,
                "Loot earned",
                "A " + boxType.getDisplayName() + " awaits from " + reason + ".",
                "/loot-boxes",
                boxId));
    }

    private LootBoxType defaultBoxType(LootBoxSource source) {
        return switch (source) {
            case STREAK_MILESTONE, ACHIEVEMENT -> LootBoxType.IRON_CHEST;
            case WEEKLY_RESET -> LootBoxType.SOVEREIGN_VAULT;
            case MANUAL -> LootBoxType.ARCANE_ORB;
        };
    }

    private Map<RewardRarity, List<RewardDefinition>> poolsByRarity() {
        Map<RewardRarity, List<RewardDefinition>> pools = new EnumMap<>(RewardRarity.class);
        for (RewardRarity rarity : RewardRarity.values()) {
            pools.put(rarity, new ArrayList<>());
        }
        for (RewardDefinition reward : rewardDefinitionRepository.findAll()) {
            pools.get(reward.getRarity()).add(reward);
        }
        return pools;
    }

    /** Roll a rarity by the box's weights, then pick uniformly from that rarity pool. */
    private RewardDefinition roll(LootBoxType boxType, Map<RewardRarity, List<RewardDefinition>> pools) {
        RewardRarity rarity = rollRarity(boxType);
        List<RewardDefinition> pool = pools.get(rarity);
        if (pool == null || pool.isEmpty()) {
            // Defensive fallback: degrade to the nearest non-empty, rarer-to-common pool.
            for (RewardRarity fallback : List.of(
                    RewardRarity.RARE, RewardRarity.COMMON, RewardRarity.LEGENDARY)) {
                List<RewardDefinition> candidate = pools.get(fallback);
                if (candidate != null && !candidate.isEmpty()) {
                    pool = candidate;
                    break;
                }
            }
        }
        if (pool == null || pool.isEmpty()) {
            throw new IllegalStateException("Reward catalog is empty");
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private RewardRarity rollRarity(LootBoxType boxType) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < boxType.getCommonWeight()) {
            return RewardRarity.COMMON;
        }
        if (roll < boxType.getCommonWeight() + boxType.getRareWeight()) {
            return RewardRarity.RARE;
        }
        return RewardRarity.LEGENDARY;
    }

    @Override
    @Transactional
    public InventoryItemDto grantInventoryItem(String userId, String rewardKey, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        RewardDefinition reward = rewardDefinitionRepository.findByKey(rewardKey)
                .orElseThrow(() -> new EntityNotFoundException("Reward not found: " + rewardKey));
        UserInventory stack = addToInventory(userId, reward, quantity);
        return new InventoryItemDto(stack.getId(), toRewardItem(reward), stack.getQuantity(), stack.isEquipped());
    }

    private void addToInventory(String userId, RewardDefinition reward) {
        addToInventory(userId, reward, 1);
    }

    private UserInventory addToInventory(String userId, RewardDefinition reward, int quantity) {
        UserInventory stack = inventoryRepository.findByUserIdAndRewardDefinitionId(userId, reward.getId())
                .orElseGet(() -> {
                    UserInventory inv = new UserInventory();
                    inv.setUserId(userId);
                    inv.setRewardDefinition(reward);
                    return inv;
                });
        stack.setQuantity(stack.getQuantity() + quantity);
        return inventoryRepository.save(stack);
    }

    private RewardItemDto toRewardItem(RewardDefinition r) {
        return new RewardItemDto(r.getId(), r.getKey(), r.getLabel(), r.getType().name(), r.getRarity().name(),
                r.getIcon(), r.getEffect());
    }

    private LootBoxDto toDto(UserLootBox box, List<RewardItemDto> contents) {
        return new LootBoxDto(box.getId(), box.getSource().name(), box.getBoxType().name(),
                box.getStatus().name(), box.getCreatedAt(), box.getOpenedAt(), contents);
    }
}
