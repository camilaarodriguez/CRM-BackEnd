package com.crmapi.sistemacrm.repository;

import com.crmapi.sistemacrm.model.Conversa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConversaRepository extends JpaRepository<Conversa, Long>, JpaSpecificationExecutor<Conversa> {

    Page<Conversa> findByVendedorId(Long vendedorId, Pageable pageable);

    java.util.Optional<Conversa> findByClienteId(Long clienteId);
}