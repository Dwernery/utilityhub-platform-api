package com.utilityhub.api.db.repository.finance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.utilityhub.api.db.entity.finance.NetWorthSnapshot;
import com.utilityhub.api.dto.response.finance.NetWorthSnapshotResponseDTO;

@Repository
public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Integer> {
    @Query("""
            SELECT new com.utilityhub.api.dto.response.finance.NetWorthSnapshotResponseDTO(
                n.balanceMonth,
                n.netWorth
            )
            FROM NetWorthSnapshot n
            ORDER BY n.balanceMonth ASC
            """)
    List<NetWorthSnapshotResponseDTO> findAllNetWorthSnapshots();
}
