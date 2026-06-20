package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.gamification.GamificationDto.InventoryItemDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.LootBoxDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardItemDto;
import com.succeshub.appdomain.model.LootBoxContent;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LootBoxServiceImpl implements LootBoxService {

    private final UserLootBoxRepository lootBoxRepository;
    private final LootBoxContentRepository contentRepository;
    private final RewardDefinitionRepository rewardDefinitionRepository;
    private final UserInventoryRepository inventoryRepository;

    @Override
    @Transactional
    public UUID grantLootBox(String userId, UserLootBox.Source source) {
        UserLootBox box = new UserLootBox();
        box.setUserId(userId);
        box.setSource(source);
        box.setStatus(UserLootBox.Status.PENDING);
        return lootBoxRepository.save(box).getId();
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

        List<RewardDefinition> all = rewardDefinitionRepository.findAll();
        List<RewardItemDto> rolled = new ArrayList<>();
        int count = 3 + ThreadLocalRandom.current().nextInt(3);
        for (int i = 0; i < count; i++) {
            RewardDefinition reward = pickReward(all);
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
    public UUID checkStreakMilestone(String userId, int streak) {
        if (streak == 7 || streak == 30 || streak == 100) {
            return grantLootBox(userId, UserLootBox.Source.STREAK_MILESTONE);
        }
        return null;
    }

    private RewardDefinition pickReward(List<RewardDefinition> all) {
        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            return all.stream().filter(r -> r.getRarity() == RewardDefinition.Rarity.LEGENDARY).findFirst()
                    .orElse(all.getFirst());
        }
        List<RewardDefinition> pool = all.stream()
                .filter(r -> r.getRarity() != RewardDefinition.Rarity.LEGENDARY)
                .toList();
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
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
        if (reward.getType() == RewardDefinition.Type.SHIELD) {
            stack.getRewardDefinition();
        }
        inventoryRepository.save(stack);
    }

    private RewardItemDto toRewardItem(RewardDefinition r) {
        return new RewardItemDto(r.getId(), r.getKey(), r.getLabel(), r.getType().name(), r.getRarity().name(), r.getIcon());
    }

    private LootBoxDto toDto(UserLootBox box, List<RewardItemDto> contents) {
        return new LootBoxDto(box.getId(), box.getSource().name(), box.getStatus().name(), box.getCreatedAt(), contents);
    }
}
