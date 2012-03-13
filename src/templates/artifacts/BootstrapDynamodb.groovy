import com.amazonaws.services.dynamodb.AmazonDynamoDBClient

class BootstrapDynamodb {
    def init = { String clientName, AmazonDynamoDBClient client -> 
    }

    def destroy = { String clientName, AmazonDynamoDBClient client ->
    }
} 
