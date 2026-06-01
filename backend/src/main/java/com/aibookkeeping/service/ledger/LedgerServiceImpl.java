package com.aibookkeeping.service.ledger;

import com.aibookkeeping.dto.LedgerRequest;
import com.aibookkeeping.entity.Ledger;
import com.aibookkeeping.entity.User;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.LedgerMapper;
import com.aibookkeeping.mapper.UserMapper;
import com.aibookkeeping.vo.LedgerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerMapper ledgerMapper;
    private final UserMapper userMapper;

    @Override
    public List<LedgerVO> listLedgers(Long userId) {
        return ledgerMapper.selectList(new LambdaQueryWrapper<Ledger>()
                .eq(Ledger::getUserId, userId)
                .orderByDesc(Ledger::getIsDefault)
                .orderByDesc(Ledger::getCreatedAt))
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerVO createLedger(LedgerRequest request, Long userId) {
        Ledger ledger = new Ledger();
        ledger.setUserId(userId);
        ledger.setName(request.getName());
        ledger.setDescription(request.getDescription());
        ledger.setIsDefault(0);
        
        // 如果是第一个账本，设为默认并更新用户当前账本
        Long count = ledgerMapper.selectCount(new LambdaQueryWrapper<Ledger>().eq(Ledger::getUserId, userId));
        if (count == 0) {
            ledger.setIsDefault(1);
        }
        
        ledgerMapper.insert(ledger);
        
        if (ledger.getIsDefault() == 1) {
            updateUserCurrentLedger(userId, ledger.getId());
        }
        
        return convertToVO(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LedgerVO updateLedger(Long id, LedgerRequest request, Long userId) {
        Ledger ledger = getAndCheckLedger(id, userId);
        ledger.setName(request.getName());
        ledger.setDescription(request.getDescription());
        ledgerMapper.updateById(ledger);
        return convertToVO(ledger);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLedger(Long id, Long userId) {
        Ledger ledger = getAndCheckLedger(id, userId);
        if (ledger.getIsDefault() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "默认账本不能删除");
        }
        
        // 如果删除的是当前正在使用的账本，切换回默认账本
        User user = userMapper.selectById(userId);
        if (user.getCurrentLedgerId() != null && user.getCurrentLedgerId().equals(id)) {
            Ledger defaultLedger = ledgerMapper.selectOne(new LambdaQueryWrapper<Ledger>()
                    .eq(Ledger::getUserId, userId)
                    .eq(Ledger::getIsDefault, 1));
            updateUserCurrentLedger(userId, defaultLedger.getId());
        }
        
        ledgerMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchLedger(Long id, Long userId) {
        getAndCheckLedger(id, userId);
        updateUserCurrentLedger(userId, id);
    }

    @Override
    public Long getCurrentLedgerId(Long userId) {
        User user = userMapper.selectById(userId);
        if (user.getCurrentLedgerId() == null) {
            // 兜底：查找默认账本，如果没有则创建一个
            Ledger defaultLedger = ledgerMapper.selectOne(new LambdaQueryWrapper<Ledger>()
                    .eq(Ledger::getUserId, userId)
                    .eq(Ledger::getIsDefault, 1));
            if (defaultLedger == null) {
                LedgerRequest req = new LedgerRequest();
                req.setName("默认账本");
                LedgerVO vo = createLedger(req, userId);
                return vo.getId();
            }
            updateUserCurrentLedger(userId, defaultLedger.getId());
            return defaultLedger.getId();
        }
        return user.getCurrentLedgerId();
    }

    private void updateUserCurrentLedger(Long userId, Long ledgerId) {
        User user = new User();
        user.setId(userId);
        user.setCurrentLedgerId(ledgerId);
        userMapper.updateById(user);
    }

    private Ledger getAndCheckLedger(Long id, Long userId) {
        Ledger ledger = ledgerMapper.selectById(id);
        if (ledger == null || !ledger.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账本不存在");
        }
        return ledger;
    }

    private LedgerVO convertToVO(Ledger ledger) {
        return LedgerVO.builder()
                .id(ledger.getId())
                .name(ledger.getName())
                .description(ledger.getDescription())
                .isDefault(ledger.getIsDefault())
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
