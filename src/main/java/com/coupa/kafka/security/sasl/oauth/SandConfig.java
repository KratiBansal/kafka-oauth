/*
 * Copyright (c) 2019 Coupa Inc, All Rights Reserved.
 * Author: John Wu
 * Email: john.wu@coupa.com
 * Created: June 06, 2019
 */

package com.coupa.kafka.security.sasl.oauth;

import com.coupa.sand.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.AppConfigurationEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import static org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule.OAUTHBEARER_MECHANISM;

public class SandConfig {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SandConfig.class);

  public static final String SAND_SERVER_URL = "sand.server.url";
  public static final String SAND_TOKEN_PATH = "sand.token_path";
  public static final String SAND_TOKEN_VERIFICATION_PATH = "sand.token_verify_path";

  public static final String SAND_CLIENT_ID = "sand.client.id";
  public static final String SAND_CLIENT_SECRET = "sand.client.secret";
  public static final String SAND_CLIENT_SCOPES = "sand.client.scopes";
  public static final String SAND_SERVICE_SCOPES = "sand.service.scopes";
  public static final String SAND_SERVICE_TARGET_SCOPES = "sand.service.target_scopes";
  public static final String SAND_SERVICE_RESOURCE = "sand.service.resource";
  public static final String SAND_SERVICE_ACTION = "sand.service.action";

  public final static String SAND_CLIENT_CACHE_KEY = "client";
  public final static int SAND_RETRY_COUNT = 3;

  public final static String KAFKA_SAND_CONFIG_PATH = "/etc/kafka/sand.properties";

  private Properties configs = new Properties();

  private static SandConfig theInstance;
  private Service sandService;

  private SandConfig() throws IOException {
      readSandConfig();
  }

  public static SandConfig getInstance() throws IOException {
      if (theInstance == null) {
          theInstance = new SandConfig();
      }
      return theInstance;
  }

  public static void configure(String saslMechanism,
                                  List<AppConfigurationEntry> jaasConfigEntries) {

    if (!OAUTHBEARER_MECHANISM.equals(saslMechanism)) {
      LOGGER.error("Unsupported SASL mechanism for OAuth");

      throw new IllegalArgumentException(
          String.format("Unsupported SASL mechanism for OAuth: %s. Please use: %s ",
              saslMechanism, OAUTHBEARER_MECHANISM));
    }

    if (Objects.requireNonNull(jaasConfigEntries).size() != 1 ||
        jaasConfigEntries.get(0) == null) {

      LOGGER.error("Must supply exactly 1 non-null JAAS mechanism configuration");

      throw new IllegalArgumentException(
          String.format("Must supply exactly 1 non-null JAAS mechanism configuration (size was %d)",
              jaasConfigEntries.size()));
    }
  }

  public Service getService() {
      if (sandService == null) {
          sandService = new Service(
              configs.getProperty(SAND_CLIENT_ID),
              configs.getProperty(SAND_CLIENT_SECRET),
              configs.getProperty(SAND_SERVER_URL),
              configs.getProperty(SAND_TOKEN_PATH),
              configs.getProperty(SAND_SERVICE_RESOURCE),
              configs.getProperty(SAND_TOKEN_VERIFICATION_PATH),
              getArray(SAND_SERVICE_SCOPES)
          );
      }
      return sandService;
  }

  public Properties getConfigs() {
    return configs;
  }

  //Use comma "," as the delimeter to split strings into an array.
  public String[] getArray(String key) {
      String[] arr = new String[0];
      String scopes = configs.getProperty(key);
      if (scopes != null) {
          arr = scopes.split(",");
      }
      return arr;
  }

  private void readSandConfig() throws IOException {
    String path = System.getenv("KAFKA_SAND_CONFIG_PATH");
    if (path == null || path.isEmpty()) {
      path = KAFKA_SAND_CONFIG_PATH;
      LOGGER.debug("Sand env config not set, falling back to default path");
    }

    File configFile = new File(path);
    if (!configFile.exists() || configFile.isDirectory()) {
      LOGGER.error("Could not read sand config file: {}", path);
      throw new IOException("Could not read sand config file: " + path);
    }

    try (InputStream input = new FileInputStream(configFile)) {
      configs.load(input);
    } catch (IOException ex) {
      LOGGER.error("Could not read property file: " + path, ex);
      throw ex;
    }
  }
}
