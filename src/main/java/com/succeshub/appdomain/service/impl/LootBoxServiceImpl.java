package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BoxTypeDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.InventoryItemDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.LootBoxDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardItemDto;
import com.succeshub.appdomain.model.LootBoxContent;
import com.succeshub.appdomain.model.LootBoxType;
import com.succeshub.appdomain.model.RewardDefinition;
import com.succeshub.appdomain.model.UserInventory;
import com.succeshub.appdomain.model.UserLootBox;
import com.succeshub.appdomain.repository.LootBoxContentRepository;
import com.succeshub.appdomain.repository.RewardDefinitionRepository;
import com.succeshub.appdomain.repository.UserInventoryRepository;
import com.succeshub.appdomain.repository.UserLootBoxRepository;
import com.succeshub.appdomain.service.LootBoxService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Override
    @Transactional
    public UUID grantLootBox(String userId, UserLootBox.Source source) {
        return createBox(userId, source, defaultBoxType(source)).getId();
    }

    @Override
    @Transactional
    public LootBoxDto grantBox(String userId, LootBoxType boxType) {
        UserLootBox box = createBox(userId, UserLootBox.Source.MANUAL, boxType);
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
        if (box.getStatus() == UserLootBox.Status.OPENED) {
            return toDto(box, contentRepository.findByLootBox_Id(box.getId()).stream()
                    .map(LootBoxContent::getRewardDefinition)
                    .map(this::toRewardItem)
                    .toList());
        }

        Map<RewardDefinition.Rarity, List<RewardDefinition>> poolByRarity = poolsByRarity();
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

        box.setStatus(UserLootBox.Status.OPENED);
        box.setOpenedAt(Instant.now());
        lootBoxRepository.save(box);
        return toDto(box, rolled);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LootBoxDto> getPendingLootBoxes(String userId) {
        return lootBoxRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, UserLootBox.Status.PENDING).stream()
                .map(b -> toDto(b, List.of()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemDto> getInventory(String userId) {
        return inventoryRepository.findByUserId(userId).stream()
                .map(i -> new InventoryItemDto(i.getId(), toRewardItem(i.getRewardDefinition()), i.getQuantity(), i.isEquipped()))
                .toList();
    }

    @Override
    @Transactional
    public InventoryItemDto toggleEquip(String userId, UUID inventoryId) {
        UserInventory item = inventoryRepository.findById(inventoryId)
                .filter(i -> i.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found"));

        RewardDefinition.Type type = item.getRewardDefinition().getType();
        if (type != RewardDefinition.Type.TITLE && type != RewardDefinition.Type.FRAME) {
            // Functional items are not equippable; return unchanged.
            return new InventoryItemDto(item.getId(), toRewardItem(item.getRewardDefinition()), item.getQuantity(), item.isEquipped());
        }

        boolean willEquip = !item.isEquipped();
        if (willEquip) {
            for (UserInventory other : inventoryRepository.findByUserId(userId)) {
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
            return createBox(userId, UserLootBox.Source.STREAK_MILESTONE, LootBoxType.IRON_CHEST).getId();
        }
        if (streak == 100) {
            return createBox(userId, UserLootBox.Source.STREAK_MILESTONE, LootBoxType.SOVEREIGN_VAULT).getId();
        }
        return null;
    }

    private UserLootBox createBox(String userId, UserLootBox.Source source, LootBoxType boxType) {
        UserLootBox box = new UserLootBox();
        box.setUserId(userId);
        box.setSource(source);
        box.setBoxType(boxType);
        box.setStatus(UserLootBox.Status.PENDING);
        return lootBoxRepository.save(box);
    }

    private LootBoxType defaultBoxType(UserLootBox.Source source) {
        return switch (source) {
            case STREAK_MILESTONE, ACHIEVEMENT -> LootBoxType.IRON_CHEST;
            case WEEKLY_RESET -> LootBoxType.SOVEREIGN_VAULT;
            case MANUAL -> LootBoxType.ARCANE_ORB;
        };
    }

    private Map<RewardDefinition.Rarity, List<RewardDefinition>> poolsByRarity() {
        Map<RewardDefinition.Rarity, List<RewardDefinition>> pools = new EnumMap<>(RewardDefinition.Rarity.class);
        for (RewardDefinition.Rarity rarity : RewardDefinition.Rarity.values()) {
            pools.put(rarity, new ArrayList<>());
        }
        for (RewardDefinition reward : rewardDefinitionRepository.findAll()) {
            pools.get(reward.getRarity()).add(reward);
        }
        return pools;
    }

    /** Roll a rarity by the box's weights, then pick uniformly from that rarity pool. */
    private RewardDefinition roll(LootBoxType boxType, Map<RewardDefinition.Rarity, List<RewardDefinition>> pools) {
        RewardDefinition.Rarity rarity = rollRarity(boxType);
        List<RewardDefinition> pool = pools.get(rarity);
        if (pool == null || pool.isEmpty()) {
            // Defensive fallback: degrade to the nearest non-empty, rarer-to-common pool.
            for (RewardDefinition.Rarity fallback : List.of(
                    RewardDefinition.Rarity.RARE, RewardDefinition.Rarity.COMMON, RewardDefinition.Rarity.LEGENDARY)) {
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

    private RewardDefinition.Rarity rollRarity(LootBoxType boxType) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < boxType.getCommonWeight()) {
            return RewardDefinition.Rarity.COMMON;
        }
        if (roll < boxType.getCommonWeight() + boxType.getRareWeight()) {
            return RewardDefinition.Rarity.RARE;
        }
        return RewardDefinition.Rarity.LEGENDARY;
    }

    private void addToInventory(String userId, RewardDefinition reward) {
        UserInventory stack = inventoryRepository.findByUserIdAndRewardDefinitionId(userId, reward.getId())
                .orElseGet(() -> {
                    UserInventory inv = new UserInventory();
                    inv.setUserId(userId);
                    inv.setRewardDefinition(reward);
                    return inv;
                });
        stack.setQuantity(stack.getQuantity() + 1);
        inventoryRepository.save(stack);
    }

    private RewardItemDto toRewardItem(RewardDefinition r) {
        return new RewardItemDto(r.getId(), r.getKey(), r.getLabel(), r.getType().name(), r.getRarity().name(),
                r.getIcon(), r.getEffect());
    }

    private LootBoxDto toDto(UserLootBox box, List<RewardItemDto> contents) {
        return new LootBoxDto(box.getId(), box.getSource().name(), box.getBoxType().name(),
                box.getStatus().name(), box.getCreatedAt(), contents);
    }
}
