# ADR 0002 — Autenticação e autorização

**Status:** aceito (Dia 1)

## Contexto
API stateless com JWT, isolamento total por usuário, papel `ADMIN` sem acesso a dados financeiros de terceiros.

## Decisão
- **HS256** com segredo de no mínimo 32 bytes (`JWT_SECRET`), validado no startup; falha rápida se ausente/curto.
- Claims: `sub` = UUID do usuário, `role`, `iat`, `exp`.
- `JwtEncoder`/`JwtDecoder` do Spring Security (Nimbus), sem filtro de validação próprio.
- O principal autenticado é um record `AuthenticatedUser(UUID id, String email, Papel role)`, montado a partir do token — **nenhuma consulta ao banco por requisição**.
- `sub` é a única fonte de identidade: `userId` vindo do corpo ou da query é ignorado.
- Registro força `role = USER`; o campo não é lido do DTO (previne mass assignment).
- `PATCH /users/me/password` exige `currentPassword`; login responde sempre "credenciais inválidas".
- Recurso de outro usuário responde **404** (não revela existência); ausência de token 401; papel insuficiente 403.
- Senha com BCrypt (strength 12) e política mínima de 12 caracteres.

## Consequências
- Mudança de papel só vale após o token expirar (TTL 1h) — aceitável e documentado.
- Troca de senha não revoga tokens existentes; mitigado pelo TTL curto. Refresh token/revogação ficam fora do MVP.
- Sem rate limiting no login no MVP (registrado como risco aceito no README).
