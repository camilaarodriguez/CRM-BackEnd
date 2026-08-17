package com.crmapi.sistemacrm.repository.specification;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> comFiltros(String busca, UsuarioRole role, Boolean ativo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.toLowerCase() + "%";
                Predicate porNome = cb.like(cb.lower(root.get("nome")), termo);
                Predicate porEmail = cb.like(cb.lower(root.get("email")), termo);
                predicates.add(cb.or(porNome, porEmail));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
