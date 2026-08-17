package com.crmapi.sistemacrm.repository.specification;

import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> comFiltros(String busca, StatusFunil status, Long vendedorId,
                                                      Boolean incluirInativos) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.toLowerCase() + "%";
                Predicate porNome = cb.like(cb.lower(root.get("nome")), termo);
                Predicate porEmail = cb.like(cb.lower(root.get("email")), termo);
                predicates.add(cb.or(porNome, porEmail));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("statusFunil"), status));
            }

            if (vendedorId != null) {
                predicates.add(cb.equal(root.get("vendedor").get("id"), vendedorId));
            }

            if (incluirInativos == null || !incluirInativos) {
                predicates.add(cb.equal(root.get("ativo"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
