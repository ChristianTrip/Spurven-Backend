package couchpotatoes.spurven.security.api;

import couchpotatoes.spurven.security.dto.LoginRequest;
import couchpotatoes.spurven.security.dto.LoginResponse;
import couchpotatoes.spurven.security.entity.UserWithRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@RestController
@RequestMapping("auth/")
public class AuthenticationController {

  @Value("${app.token-issuer}")
  private String tokenIssuer;

  @Value("${app.token-expiration}")
  private long tokenExpiration;
  private final AuthenticationManager authenticationManager;

  @Autowired
  JwtEncoder encoder;

  public AuthenticationController(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    System.out.println(request.getUsername());
    System.out.println(request.getPassword());
    try {
      UsernamePasswordAuthenticationToken uat = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
      Authentication authentication = authenticationManager.authenticate(uat);

      UserWithRoles user = (UserWithRoles) authentication.getPrincipal();
      Instant now = Instant.now();
      long expiry = tokenExpiration;
      String scope = authentication.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .collect(joining(" "));

      JwtClaimsSet claims = JwtClaimsSet.builder()
              .issuer(tokenIssuer)  //Only this for simplicity
              .issuedAt(now)
              .expiresAt(now.plusSeconds(tokenExpiration))
              .subject(user.getUsername())
              .claim("roles",scope)
              .build();
      JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
      String token = encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();


      List<String> roles = user.getRoles().stream().map(role->role.toString()).collect(Collectors.toList());
      return ResponseEntity.ok()
              .body(new LoginResponse(user.getUsername(),token,roles));
    } catch (BadCredentialsException ex) {
      System.out.println("In the catch statement");
      throw ex;
      //throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Username or password wrong");
    }
  }
}