package com.devikapps.caverne.modules.user;

public abstract class AbstractExternalAuthenticationProvider implements AuthenticationProvider {

  private final UserIdentityService userIdentityService;

  protected AbstractExternalAuthenticationProvider(UserIdentityService userIdentityService) {
    this.userIdentityService = userIdentityService;
  }

  @Override
  public final UserAccount authenticate(String token) {
    return userIdentityService.resolveOrCreateExternalUser(verifyIdentity(token));
  }

  protected final java.util.Map<String, Object> asMap(Object rawValue) {
    return userIdentityService.asMap(rawValue);
  }

  protected abstract ExternalIdentityProfile verifyIdentity(String token);
}
