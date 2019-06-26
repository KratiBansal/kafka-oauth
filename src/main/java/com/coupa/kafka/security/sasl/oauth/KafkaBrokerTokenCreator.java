/*
 * Copyright (c) 2019 Coupa Inc, All Rights Reserved.
 * Author: John Wu
 * Email: john.wu@coupa.com
 * Created: June 06, 2019
 */

package com.coupa.kafka.security.sasl.oauth;

import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coupa.sand.TokenResponse;

public class KafkaBrokerTokenCreator implements AuthenticateCallbackHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KafkaBrokerTokenCreator.class);

  @Override
  public void configure(Map<String, ?> configs, String saslMechanism,
                        List<AppConfigurationEntry> jaasConfigEntries)  {

    SandConfig.configure(saslMechanism, jaasConfigEntries);
    LOGGER.info("Configured Kafka broker oauth token creator successfully");
  }

  @Override
  public void close() {
  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    for (Callback callback : callbacks) {

      if (callback instanceof OAuthBearerTokenCallback) {
        makeToken((OAuthBearerTokenCallback) callback);
      } else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }

  private void makeToken(OAuthBearerTokenCallback callback) throws IOException {
    if (callback.token() != null) {
      throw new IllegalArgumentException("OAuth token is already set.");
    }

    SandConfig sand = SandConfig.getInstance();

    LOGGER.info("Getting token from SAND");
    TokenResponse tokenResponse = sand.getService().getOAuthToken(
            SandConfig.SAND_CLIENT_CACHE_KEY,
            sand.getArray(SandConfig.SAND_CLIENT_SCOPES),
            SandConfig.SAND_RETRY_COUNT);

    if (tokenResponse == null) {
      throw new IllegalArgumentException("Failed to get token from SAND");
    }
    LOGGER.debug("Token received: {}", tokenResponse.getToken());

    callback.token(new SandOAuthToken(sand.getService().getClientId().toString(), tokenResponse));
  }

}
