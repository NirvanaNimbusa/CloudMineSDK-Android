package com.cloudmine.api.rest;

import com.cloudmine.api.CMUser;
import com.cloudmine.api.CMUserToken;
import com.cloudmine.api.SimpleCMObject;
import com.cloudmine.api.StoreIdentifier;
import com.cloudmine.api.rest.callbacks.LoginResponseCallback;
import com.cloudmine.api.rest.callbacks.ObjectModificationResponseCallback;
import com.cloudmine.api.rest.callbacks.SimpleCMObjectResponseCallback;
import com.cloudmine.api.rest.response.LogInResponse;
import com.cloudmine.api.rest.response.ObjectModificationResponse;
import com.cloudmine.api.rest.response.SimpleCMObjectResponse;
import com.cloudmine.test.CloudMineTestRunner;
import com.cloudmine.test.ServiceTestBase;
import com.cloudmine.test.TestServiceCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static com.cloudmine.test.AsyncTestResultsCoordinator.reset;
import static com.cloudmine.test.AsyncTestResultsCoordinator.waitThenAssertTestResults;
import static junit.framework.Assert.*;

/**
 * Copyright CloudMine LLC
 * User: johnmccarthy
 * Date: 6/13/12, 3:51 PM
 */
@RunWith(CloudMineTestRunner.class)
public class CMStoreIntegrationTest extends ServiceTestBase {
    private CMStore store;
    @Before
    public void setUp() {
        super.setUp();
        store = CMStore.CMStore();
    }

    @Test
    public void testSaveObject() {
        final SimpleCMObject object = SimpleCMObject.SimpleCMObject();
        object.add("bool", true);


        store.saveObject(object, TestServiceCallback.testCallback(new ObjectModificationResponseCallback() {
            public void onCompletion(ObjectModificationResponse response) {

                SimpleCMObjectResponse loadResponse = CMWebService.service().get(object.key());
                SimpleCMObject loadedObject = loadResponse.object(object.key());
                assertNotNull(loadedObject);

                assertEquals(object, loadedObject);
                assertEquals(StoreIdentifier.DEFAULT, object.savedWith());

            }
        }));
        waitThenAssertTestResults();
    }

    @Test
    public void testSaveUserObject() {
        final SimpleCMObject object = SimpleCMObject.SimpleCMObject();
        object.add("bool", true);
        CMUser user = CMUser.CMUser("dfljdsfkdfskd@t.com", "t");
        CMWebService.service().set(user);
        final CMUserToken token = CMWebService.service().login(user).userToken();

        object.saveWith(new StoreIdentifier(token));
        CMStore store = CMStore.CMStore();
        store.setLoggedInUser(token);

        store.saveObject(object, TestServiceCallback.testCallback(new ObjectModificationResponseCallback() {
            public void onCompletion(ObjectModificationResponse ignoredResponse) {
                assertTrue(ignoredResponse.wasSuccess());
                SimpleCMObjectResponse response = CMWebService.service().userWebService(token).get(object.key());
                assertTrue(response.wasSuccess());
                SimpleCMObject loadedObject = response.object(object.key());
                assertNotNull(loadedObject);
                assertEquals(object, loadedObject);
            }
        }));
        waitThenAssertTestResults();
    }

    @Test
    public void testUserLogin() {
        CMUser user = user();
        service.set(user);
        CMUserToken token = service.login(user).userToken();
        service.userWebService(token).set(SimpleCMObject.SimpleCMObject("key").add("k", "v").asJson());
        reset(2);
        store.login(user, TestServiceCallback.testCallback(new LoginResponseCallback() {
            public void onCompletion(LogInResponse response) {
                assertTrue(response.wasSuccess());
                store.allUserObjects(TestServiceCallback.testCallback(new SimpleCMObjectResponseCallback() {
                    public void onCompletion(SimpleCMObjectResponse response) {
                        assertTrue(response.wasSuccess());
                        System.out.println("in here");
                        assertEquals("v", response.object("key").getString("k"));
                    }
                }));
            }
        }));

        waitThenAssertTestResults();

    }

    @Test
    public void testLoadUserObjects() {
        SimpleCMObject appObject = SimpleCMObject.SimpleCMObject();
        appObject.add("SomeKey", "Value");


        service.set(appObject.asJson());

        CMUser user = user();
        service.set(user);
        CMUserToken token = service.login(user).userToken();

        final List<SimpleCMObject> userObjects = new ArrayList<SimpleCMObject>();
        UserCMWebService userService = service.userWebService(token);
        for(int i = 0; i < 5; i++) {
            SimpleCMObject userObject = SimpleCMObject.SimpleCMObject();
            userObject.add("integer", i);
            userObjects.add(userObject);
            userService.set(userObject.asJson());
        }
        store.setLoggedInUser(token);
        store.allUserObjects(TestServiceCallback.testCallback(new SimpleCMObjectResponseCallback() {
           public void onCompletion(SimpleCMObjectResponse response) {
               assertTrue(response.wasSuccess());
               assertEquals(userObjects.size(), response.objects().size());
               for(SimpleCMObject object : userObjects) {
                   assertEquals(object, response.object(object.key()));
               }
           }
        }));
        waitThenAssertTestResults();
    }
}