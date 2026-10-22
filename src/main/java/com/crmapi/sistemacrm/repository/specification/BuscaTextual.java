package com.crmapi.sistemacrm.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Optional;

/**
 * Trecho comum aos filtros de cliente e de usuario: as duas telas tem um unico
 * campo de busca que procura ao mesmo tempo no nome e no e-mail, sem diferenciar
 * maiusculas de minusculas.
 *
 * Ficava copiado nas duas Specifications; concentrar aqui garante que mudar o
 * criterio de busca mude nos dois lugares de uma vez.
 */
final class BuscaTextual {

    private BuscaTextual() {
    }

    /**
     * Predicado "o nome ou o e-mail contem o termo". Devolve vazio quando nao ha
     * o que buscar, para que o filtro simplesmente nao entre na consulta.
     */
    static Optional<Predicate> porNomeOuEmail(Root<?> root, CriteriaBuilder cb, String busca) {
        if (busca == null || busca.isBlank()) {
            return Optional.empty();
        }
        String termo = "%" + busca.toLowerCase() + "%";
        Predicate porNome = cb.like(cb.lower(root.get("nome")), termo);
        Predicate porEmail = cb.like(cb.lower(root.get("email")), termo);
        return Optional.of(cb.or(porNome, porEmail));
    }
}
