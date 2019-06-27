/*
 * Copyright (c) 2019 Coupa Inc, All Rights Reserved.
 * Author: John Wu
 * Email: john.wu@coupa.com
 * Created: June 06, 2019
 */

package com.coupa.kafka.security.sasl.oauth;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.coupa.sand.AllowedResponse;
import com.coupa.sand.TokenResponse;

public class SandOAuthToken implements OAuthBearerToken {
  private String token;
  private Set<String> scope = new TreeSet<String>();
  private long lifetimeMs = 0L;
  private String principalName = "";
  private Long startTimeMs = 0L;


  public SandOAuthToken(String token, AllowedResponse resp) {
    this.token = token;
    if (!resp.isAllowed()) {
      return;
    }
    scope = new TreeSet<String>(Arrays.asList(resp.getScopes()));
    principalName = resp.getSub();

    String expireAt = resp.getExp();
    Instant exp = Instant.parse(expireAt);
    lifetimeMs = exp.toEpochMilli();

    String issuedAt = resp.getIat();
    Instant inst = Instant.parse(issuedAt);
    startTimeMs = inst.toEpochMilli();
  }

  public SandOAuthToken(String clientID, TokenResponse resp) {
    token = resp.getToken();
    principalName = clientID;
    scope = new TreeSet<String>(Arrays.asList(resp.getScopes()));
    
    startTimeMs = Instant.now().toEpochMilli();
    lifetimeMs = startTimeMs + resp.getExpiresIn() * 1_000L;
  }

  @Override
  public String value() {
    return token;
  }

  @Override
  public Set<String> scope() {
    return scope;
  }

  @Override
  public long lifetimeMs() {
    return lifetimeMs;
  }

  @Override
  public String principalName() {
    return principalName;
  }

  @Override
  public Long startTimeMs() {
    return startTimeMs;
  }

  @Override
  public String toString() {
    return "SandOAuthToken{" +
        "value='" + value() + '\'' +
        ", lifetimeMs=" + lifetimeMs() +
        ", principalName='" + principalName() + '\'' +
        ", startTimeMs=" + startTimeMs() +
        ", scope=" + scope() +
        '}';
  }
}
