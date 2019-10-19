/*
 * Copyright (c) 2019 Coupa Inc, All Rights Reserved.
 * Author: John Wu
 * Email: john.wu@coupa.com
 * Created: June 06, 2019
 */

package com.coupa.kafka.security.sasl.oauth;

import com.coupa.sand.AllowedResponse;
import com.coupa.sand.VerificationOptions;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class KafkaTokenValidator implements AuthenticateCallbackHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(KafkaTokenValidator.class);

  @Override
  public void configure(Map<String, ?> configs, String saslMechanism,
      List<AppConfigurationEntry> jaasConfigEntries) {

    SandConfig.configure(saslMechanism, jaasConfigEntries);
    LOGGER.info("Configured Kafka token validator successfully");
  }

  @Override
  public void close() {
  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    for (Callback callback : callbacks) {
      if (callback instanceof OAuthBearerValidatorCallback) {
        try {
          OAuthBearerValidatorCallback validationCallback = (OAuthBearerValidatorCallback) callback;
          validateToken(validationCallback);
        } catch (KafkaException e) {
          throw new IOException(e.getMessage(), e);
        }
      } else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }

  private void validateToken(OAuthBearerValidatorCallback callback) throws IOException {
    String token = callback.tokenValue();

    SandConfig sand = SandConfig.getInstance();
    Properties sandConfig = sand.getConfigs();

    VerificationOptions options = new VerificationOptions(
        sand.getArray(SandConfig.SAND_SERVICE_TARGET_SCOPES),
        sandConfig.getProperty(SandConfig.SAND_SERVICE_ACTION),
        sandConfig.getProperty(SandConfig.SAND_SERVICE_RESOURCE));

    AllowedResponse resp;
    try {
      resp = sand.getService().checkToken(token, options);
    } catch (Exception e) {
      LOGGER.error("Error verifying Sand token: " + e.getMessage());
      callback.error("temporarily_unavailable", String.join(",", sand.getArray(SandConfig.SAND_SERVICE_TARGET_SCOPES)), null);
      return;
    }
    SandOAuthToken tk = new SandOAuthToken(token, resp);
    if (resp.isAllowed()) {
      LOGGER.debug("Sand access granted for: " + tk.principalName());
      callback.token(tk);
    } else {
      LOGGER.info("Sand token access denied: " + token);
      callback.error("access_denied", String.join(",", sand.getArray(SandConfig.SAND_SERVICE_TARGET_SCOPES)), null);
    }
  }
}
