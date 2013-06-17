
DynamoDB support
----------------

Plugin page: [http://artifacts.griffon-framework.org/plugin/dynamodb](http://artifacts.griffon-framework.org/plugin/dynamodb)


The Dynamodb plugin enables lightweight access to [Amazon's DynamoDB][1] databases.
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * DynamodbConfig.groovy - contains the database definitions.
 * BootstrapDynamodb.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withDynamodb` will be injected into all controllers,
giving you access to a `com.amazonaws.services.dynamodb.AmazonDynamoDBClient` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

This method is aware of multiple databases. If no databaseName is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDatabases = {
            withDynamodb { databaseName, client -> ... }
            withDynamodb('internal') { databaseName, client -> ... }
        }
    }

This method is also accessible to any component through the singleton `griffon.plugins.dynamodb.DynamodbConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`DynamodbEnhancer.enhance(metaClassInstance, dynamodbProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withDynamodb()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.dynamodb.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * DynamodbConnectStart[config, databaseName] - triggered before connecting to the database
 * DynamodbConnectEnd[databaseName, client] - triggered after connecting to the database
 * DynamodbDisconnectStart[config, databaseName, client] - triggered before disconnecting from the database
 * DynamodbDisconnectEnd[config, databaseName] - triggered after disconnecting from the database

### Multiple Stores

The config file `DynamodbConfig.groovy` defines a default client block. As the name
implies this is the client used by default, however you can configure named clients
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    clients {
        internal {
            credentials {
                accessKey = '*****'
                secretKey = '*****'
            }
        }
    }

This block can be used inside the `environments()` block in the same way as the
default client block is used.

### Credentials

Access and secret keys can be obtained from [https://aws-portal.amazon.com/gp/aws/securityCredentials][2] after you have successfully
signed in for usage of the Dynamodb services provided by Amazon.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/dynamodb][3]

Testing
-------
The `withDynamodb()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `DynamodbEnhancer.enhance(metaClassInstance, dynamodbProviderInstance)` where 
`dynamodbProviderInstance` is of type `griffon.plugins.dynamodb.DynamodbProvider`. The contract for this interface looks like this

    public interface DynamodbProvider {
        Object withDynamodb(Closure closure);
        Object withDynamodb(String clientName, Closure closure);
        <T> T withDynamodb(CallableWithArgs<T> callable);
        <T> T withDynamodb(String clientName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyDynamodbProvider implements DynamodbProvider {
        Object withDynamodb(String clientName = 'default', Closure closure) { null }
        public <T> T withDynamodb(String clientName = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            DynamodbEnhancer.enhance(service.metaClass, new MyDynamodbProvider())
            // exercise service methods
        }
    }


[1]: http://aws.amazon.com/dynamodb/
[2]: https://aws-portal.amazon.com/gp/aws/securityCredentials
[3]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/dynamodb

