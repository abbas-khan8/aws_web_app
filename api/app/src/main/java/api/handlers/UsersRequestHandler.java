package api.handlers;

import api.models.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.simple.JSONObject;

public class UsersRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final String DYNAMO_TABLE = "Users";
    private final AmazonDynamoDB dynamoClient = AmazonDynamoDBClientBuilder.defaultClient();
    private final DynamoDB dynamo = new DynamoDB(dynamoClient);

    public Item getUser(APIGatewayProxyRequestEvent input){
        int id;
        Item dbItem = null;

        try{
            if(input.getPathParameters() != null){
                var pathParams = input.getPathParameters();
                if(pathParams.containsKey("id")){
                    id = Integer.parseInt((String) pathParams.get("id"));
                    dbItem = dynamo.getTable(DYNAMO_TABLE).getItem("id", id);
                }
            } else if(input.getQueryStringParameters() != null){
                var queryParams = input.getQueryStringParameters();
                if(queryParams.containsKey("id")){
                    id = Integer.parseInt((String) queryParams.get("id"));
                    dbItem = dynamo.getTable(DYNAMO_TABLE).getItem("id", id);
                }
            }
            return dbItem;
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        var responseBody = new JSONObject();
        var response = new APIGatewayProxyResponseEvent();

        Item dbItem;

        try{
            dbItem = getUser(input);

            if(dbItem != null){
                User user = new User(dbItem.toJSON());
                responseBody.put("user", user.toJson());
                response.setBody(responseBody.toString());
                response.setStatusCode(200);
            }else {
                responseBody.put("message", "No users found");
                response.setBody(responseBody.toString());
                response.setStatusCode(404);
            }
        }catch (Exception e){
            var error = "ERROR: " + e.getMessage();
            context.getLogger().log(error);

            response.setBody(error);
            response.setStatusCode(400);
        }

        return response
                .withIsBase64Encoded(false);
    }

    public APIGatewayProxyResponseEvent handlePutRequest(APIGatewayProxyRequestEvent input, Context context) {
        var responseBody = new JSONObject();
        var response = new APIGatewayProxyResponseEvent();

        try {
            if(!input.getBody().isEmpty()){
                var dbItem = getUser(input);

                if(dbItem != null){
                    var id = dbItem.getInt("id");
                    User newUser = new User(id, (String) input.getBody());

                    UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                            .withPrimaryKey("id", id)
                            .withUpdateExpression("set username = :val")
                            .withValueMap(new ValueMap()
                                    .withString(":val", newUser.getUserName()));

                    dynamo.getTable(DYNAMO_TABLE).updateItem(updateItemSpec);

                    updateItemSpec = new UpdateItemSpec()
                            .withPrimaryKey("id", id)
                            .withUpdateExpression("set email = :val")
                            .withValueMap(new ValueMap()
                                    .withString(":val", newUser.getEmail()));

                    dynamo.getTable(DYNAMO_TABLE).updateItem(updateItemSpec);

                    responseBody.put("user", newUser.toJson());
                    response.setBody(responseBody.toString());
                    response.setStatusCode(200);
                } else {
                    User newUser = new User((String) input.getBody());

                    dynamo.getTable(DYNAMO_TABLE)
                            .putItem(new PutItemSpec()
                                    .withItem(new Item()
                                            .withNumber("id", newUser.getId())
                                            .withString("username", newUser.getUserName())
                                            .withString("email", newUser.getEmail())));

                    responseBody.put("user", newUser.toJson());
                    response.setBody(responseBody.toString());
                    response.setStatusCode(200);
                }

            }
        } catch (Exception e){
            var error = "ERROR: " + e.getMessage();
            context.getLogger().log(error);
            responseBody.put("error", e.getMessage());
            response.setBody(responseBody.toString());
            response.setStatusCode(400);
        }
        return response
                .withIsBase64Encoded(false);
    }

    public APIGatewayProxyResponseEvent handleDeleteRequest(APIGatewayProxyRequestEvent input, Context context) {
        var responseBody = new JSONObject();
        var response = new APIGatewayProxyResponseEvent();

        Item dbItem;

        try {
            dbItem = getUser(input);

            if(dbItem != null){
                dynamo.getTable(DYNAMO_TABLE)
                        .deleteItem("id", dbItem.getInt("id"));
                response.setStatusCode(204);
            }else {
                responseBody.put("message", "No user found");
                response.setBody(responseBody.toString());
                response.setStatusCode(404);
            }
        } catch (Exception e){
            var error = "ERROR: " + e.getMessage();
            responseBody.put("error", e.getMessage());
            response.setBody(responseBody.toString());
            response.setStatusCode(400);
            context.getLogger().log(error);
        }
        return response
                .withIsBase64Encoded(false);
    }
}
