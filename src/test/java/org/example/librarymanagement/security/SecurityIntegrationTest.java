package org.example.librarymanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.librarymanagement.entity.Role;
import org.example.librarymanagement.entity.User;
import org.example.librarymanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests — uses the REAL SecurityFilterChain, JwtAuthenticationFilter,
 * and Spring Security configuration. No mocking of security components.
 *
 * Covered scenarios:
 *   1. Protected endpoint without Authorization header → 401
 *   2. ADMIN endpoint with USER token → 403
 *   3. Expired JWT token → 401
 *   4. Malformed JWT (not parseable) → 401
 *   5. Valid JWT signed with wrong key (invalid signature) → 401
 *   6. Valid ADMIN token on ADMIN endpoint → 200
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security – Integration Testlər")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User userPrincipal;
    private User adminPrincipal;

    /** A protected endpoint that requires USER or ADMIN role (GET is allowed for both). */
    private static final String PROTECTED_GET = "/api/v1/authors";

    /** An endpoint that requires ADMIN role. */
    private static final String ADMIN_ONLY = "/api/v1/admin/users";

    @BeforeEach
    void setUp() {
        // Create a USER
        userPrincipal = userRepository.save(User.builder()
                .username("sec_user_" + System.nanoTime())
                .email("sec_user_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .role(Role.USER)
                .enabled(true)
                .build());

        // Create an ADMIN
        adminPrincipal = userRepository.save(User.builder()
                .username("sec_admin_" + System.nanoTime())
                .email("sec_admin_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }

    // -------------------------------------------------------------------------
    // 1. Protected endpoint WITHOUT Authorization header → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. Token olmadan qorunan endpoint → 401 Unauthorized")
    void noToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_GET)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value(PROTECTED_GET));
    }

    // -------------------------------------------------------------------------
    // 2. ADMIN endpoint with a valid USER token → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. USER token ilə ADMIN endpoint → 403 Forbidden")
    void userTokenOnAdminEndpoint_returnsForbidden() throws Exception {
        String userToken = jwtService.generateToken(userPrincipal);

        mockMvc.perform(get(ADMIN_ONLY)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // 3. Expired JWT token → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. Vaxtı bitmiş JWT token → 401 Unauthorized")
    void expiredToken_returnsUnauthorized() throws Exception {
        String expiredToken = buildExpiredToken(adminPrincipal);

        mockMvc.perform(get(PROTECTED_GET)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Token vaxtı bitib")));
    }

    // -------------------------------------------------------------------------
    // 4. Malformed JWT (random string, not a JWT) → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. Yanlış formatlı JWT (malformed) → 401 Unauthorized")
    void malformedToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(PROTECTED_GET)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this-is-not-a-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // 5. Valid structure JWT but signed with a DIFFERENT key → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. Yanlış açarla imzalanmış JWT (invalid signature) → 401 Unauthorized")
    void invalidSignatureToken_returnsUnauthorized() throws Exception {
        String wrongKeyToken = buildTokenWithDifferentKey(adminPrincipal);

        mockMvc.perform(get(PROTECTED_GET)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongKeyToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // 6. Valid ADMIN token on protected endpoint → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. Etibarlı ADMIN token ilə qorunan endpoint → 200 OK")
    void validAdminToken_returnsOk() throws Exception {
        String adminToken = jwtService.generateToken(adminPrincipal);

        mockMvc.perform(get(PROTECTED_GET)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    // -------------------------------------------------------------------------
    // 7. Valid USER token on read endpoint → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. Etibarlı USER token ilə oxuma endpoint-i → 200 OK")
    void validUserToken_onReadEndpoint_returnsOk() throws Exception {
        String userToken = jwtService.generateToken(userPrincipal);

        mockMvc.perform(get(PROTECTED_GET)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a properly signed JWT that expired 1 second ago. */
    private String buildExpiredToken(User user) {
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() - 1000); // 1 second in the past

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date(now.getTime() - 60_000))
                .expiration(expiredAt)
                .signWith(resolveSigningKey(jwtSecret))
                .compact();
    }

    /** Builds a structurally valid JWT but signed with a completely different key. */
    private String buildTokenWithDifferentKey(User user) {
        // A different 32-byte base64-encoded key
        String wrongSecret = "d3Jvbmctc2VjcmV0LWtleS1mb3ItdGVzdGluZy1zaWduYXR1cmUtb25seQ==";

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(resolveSigningKey(wrongSecret))
                .compact();
    }

    private SecretKey resolveSigningKey(String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
