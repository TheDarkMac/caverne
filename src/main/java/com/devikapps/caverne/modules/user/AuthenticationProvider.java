package com.devikapps.caverne.modules.user;

public interface AuthenticationProvider {

  String getProviderCode();

  boolean supportsToken(String token);

  UserAccount authenticate(String token);

  default void logout(String token) {}
}
