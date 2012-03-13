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

import griffon.util.CallableWithArgs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Andres Almiray
 */
final class DynamodbEnhancer {
    private static final Logger LOG = LoggerFactory.getLogger(DynamodbEnhancer)

    private DynamodbEnhancer() {}
    
    static void enhance(MetaClass mc, DynamodbProvider provider = DynamodbClientHolder.instance) {
        if(LOG.debugEnabled) LOG.debug("Enhancing $mc with $provider")
        mc.withDynamodb = {Closure closure ->
            provider.withDynamodb('default', closure)
        }
        mc.withDynamodb << {String clientName, Closure closure ->
            provider.withDynamodb(clientName, closure)
        }
        mc.withDynamodb << {CallableWithArgs callable ->
            provider.withDynamodb('default', callable)
        }
        mc.withDynamodb << {String clientName, CallableWithArgs callable ->
            provider.withDynamodb(clientName, callable)
        }
    }
}
