/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package griffon.plugins.dynamodb

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient
import com.amazonaws.services.dynamodb.AmazonDynamoDBAsyncClient

import griffon.core.GriffonApplication
import griffon.util.Environment
import griffon.util.Metadata
import griffon.util.CallableWithArgs

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
@Singleton
final class DynamodbConnector implements DynamodbProvider {
    private bootstrap

    private static final Logger LOG = LoggerFactory.getLogger(DynamodbConnector)

    Object withDynamodb(String clientName = 'default', Closure closure) {
        DynamodbClientHolder.instance.withDynamodb(clientName, closure)
    }

    public <T> T withDynamodb(String clientName = 'default', CallableWithArgs<T> callable) {
        return DynamodbClientHolder.instance.withDynamodb(clientName, callable)
    }

    // ======================================================

    ConfigObject createConfig(GriffonApplication app) {
        def clientClass = app.class.classLoader.loadClass('DynamodbConfig')
        new ConfigSlurper(Environment.current.name).parse(clientClass)
    }

    private ConfigObject narrowConfig(ConfigObject config, String clientName) {
        return clientName == 'default' ? config.client : config.clients[clientName]
    }

    AmazonDynamoDBClient connect(GriffonApplication app, ConfigObject config, String clientName = 'default') {
        if (DynamodbClientHolder.instance.isClientConnected(clientName)) {
            return DynamodbClientHolder.instance.getClient(clientName)
        }

        config = narrowConfig(config, clientName)
        app.event('DynamodbConnectStart', [config, clientName])
        AmazonDynamoDBClient client = startDynamodb(config, clientName)
        DynamodbClientHolder.instance.setClient(clientName, client)
        bootstrap = app.class.classLoader.loadClass('BootstrapDynamodb').newInstance()
        bootstrap.metaClass.app = app
        bootstrap.init(clientName, client)
        app.event('DynamodbConnectEnd', [clientName, client])
        client
    }

    void disconnect(GriffonApplication app, ConfigObject config, String clientName = 'default') {
        if (DynamodbClientHolder.instance.isClientConnected(clientName)) {
            config = narrowConfig(config, clientName)
            AmazonDynamoDBClient client = DynamodbClientHolder.instance.getClient(clientName)
            app.event('DynamodbDisconnectStart', [config, clientName, client])
            bootstrap.destroy(clientName, client)
            stopDynamodb(config, client)
            app.event('DynamodbDisconnectEnd', [config, clientName])
            DynamodbClientHolder.instance.disconnectClient(clientName)
        }
    }

    private AmazonDynamoDBClient startDynamodb(ConfigObject config, String clientName) {
        ClientConfiguration clientConfig = new ClientConfiguration()
        config.config.each { k, v -> clientConfig[k] = v }
        AWSCredentials credentials = createCredentials(config, clientName)
        AmazonDynamoDBClient client = config.async ? new AmazonDynamoDBAsyncClient(credentials) : new AmazonDynamoDBClient(credentials)
        client.configuration = clientConfig
        client
    }

    private void stopDynamodb(ConfigObject config, AmazonDynamoDBClient client) {
        // empty ??
    }
    
    private createCredentials(ConfigObject config, String clientName) {
        if(config.credentialsProvider && config.credentialsProvider instanceof Class) return config.credentialsProvider.newInstance()
        if(config.credentials) return new DynamodbCredentials(config.credentials.accessKey, config.credentials.secretKey)
        throw new IllegalArgumentException("Invalid credentials configuration for client $clientName")
    }
    
    private static class DynamodbCredentials implements AWSCredentials {
        private final String accessKey
        private final String secretKey
        
        DynamodbCredentials(String accessKey, String secretKey) {
            this.accessKey = accessKey
            this.secretKey = secretKey
        }
        
        String getAWSAccessKeyId() { accessKey }
        String getAWSSecretKey() { secretKey }
    }
}
