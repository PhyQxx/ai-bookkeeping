package com.aibookkeeping.mapper;

import com.aibookkeeping.entity.Bill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    @Select("""
            SELECT b.remark, c.name as categoryName, COUNT(*) as count
            FROM bill b
            JOIN category c ON b.category_id = c.id
            WHERE b.user_id = #{userId} AND b.ledger_id = #{ledgerId} AND b.remark IS NOT NULL AND b.remark != ''
            GROUP BY b.remark, c.name
            HAVING count >= 2
            ORDER BY count DESC
            LIMIT 20
            """)
    List<Map<String, Object>> selectFrequentCategoryMappingsByLedger(@Param("userId") Long userId, @Param("ledgerId") Long ledgerId);
}
