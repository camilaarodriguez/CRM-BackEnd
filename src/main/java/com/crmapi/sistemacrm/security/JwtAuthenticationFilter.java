package com.crmapi.sistemacrm.security;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Le o cabecalho "Authorization: Bearer <token>" e, se o token for valido e o
 * usuario continuar ativo, registra a autenticacao no contexto do Spring Security.
 *
 * Quando nao ha token, ou ele e invalido, o filtro apenas deixa a requisicao seguir
 * sem autenticacao: quem devolve o 401 e a propria cadeia de seguranca, nas rotas
 * que exigem login. O usuario e recarregado do banco a cada requisicao para que
 * desativar alguem corte o acesso na hora, sem esperar o token expirar.
 *
 * Nao e um @Component de proposito: e instanciado pelo SegurancaConfig, para nao
 * ser registrado duas vezes como filtro do servlet.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.extrairEmail(cabecalho.substring(PREFIXO.length()))
                    .flatMap(usuarioRepository::findByEmail)
                    .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                    .ifPresent(JwtAuthenticationFilter::autenticar);
        }

        chain.doFilter(request, response);
    }

    private static void autenticar(Usuario usuario) {
        UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                usuario.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}
