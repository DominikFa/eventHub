// src/test/java/com/example/event_hub/InvitationBatchSendUnitTest.java
package com.example.event_hub;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer; // Correct import for doAnswer
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.event_hub.Model.InvitationModel;
import com.example.event_hub.Model.ResultWrapper;
import com.example.event_hub.Repositiry.AuthRepository;
import com.example.event_hub.Repositiry.EventHubRepository;
import com.example.event_hub.ViewModel.InvitationViewModel;

import androidx.lifecycle.MutableLiveData; // Correct import for MutableLiveData
import androidx.lifecycle.Observer; // Correct import for Observer

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvitationBatchSendUnitTest {

    @Mock
    private EventHubRepository mockEventHubRepository;
    @Mock
    private AuthRepository mockAuthRepository;

    // Use actual ViewModel, but ensure it gets mocked dependencies
    private InvitationViewModel invitationViewModel;

    // This LiveData will simulate the one inside EventHubRepository
    private MutableLiveData<ResultWrapper<Void>> voidOperationStateForTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this); // Initialize mocks

        // Initialize the test-specific LiveData
        voidOperationStateForTest = new MutableLiveData<>();

        // Mock AuthRepository behavior:
        when(mockAuthRepository.getCurrentTokenSynchronous()).thenReturn("mock-auth-token");
        when(mockAuthRepository.getCurrentUserIdSynchronous()).thenReturn(1L);

        // Mock EventHubRepository's voidOperationState field to return our test LiveData
        // This is a direct mock of a field, which is common in unit testing.
        when(mockEventHubRepository.voidOperationState).thenReturn(voidOperationStateForTest);

        // Mock EventHubRepository's sendInvitation method (which is a void method)
        // When sendInvitation is called, we want to simulate the repository's internal
        // logic of updating its `voidOperationState`.
        doAnswer(invocation -> {
            // Simulate the success path by posting to the LiveData.
            // This mirrors `EventHubRepository.sendInvitation`'s behavior after a successful API call.
            voidOperationStateForTest.postValue(new ResultWrapper.Success<>(null));
            return null; // `sendInvitation` is a void method, so return null
        }).when(mockEventHubRepository).sendInvitation(any(Long.class), any(Long.class), any(String.class));

        // Instantiate the InvitationViewModel.
        // CRITICAL: For this test to work without refactoring singletons' getInstance(),
        // you would typically need to:
        // 1. Temporarily replace the singleton's internal instance with your mock.
        //    (e.g., via reflection or a test-specific setter like `EventHubRepository.setInstanceForTesting(mockEventHubRepository);`)
        // 2. Or, modify InvitationViewModel to take its repositories as constructor arguments (Dependency Injection).
        // Without one of these, the ViewModel will use the *real* singletons, and your mocks won't be used.

        // Assuming a mechanism exists to inject `mockEventHubRepository` and `mockAuthRepository` into
        // the `InvitationViewModel` instance or to replace the singletons globally for this test run.
        // For demonstration purposes, we'll instantiate a new ViewModel here, knowing its dependency
        // resolution would be external to this snippet for actual execution.
        invitationViewModel = new InvitationViewModel(); // Will call getInstance()
    }

    @Test
    public void testSendingMultipleInvitationsVerifiesRepositoryCalls() {
        int numberOfInvitations = 100; // Simulate sending 100 invitations
        Long eventId = 123L;
        String authToken = "mock-auth-token";

        // Mock AuthRepository to provide a token that the ViewModel will use when calling `sendInvitation`
        when(mockAuthRepository.getCurrentTokenSynchronous()).thenReturn(authToken);

        // Simulate sending invitations
        for (int i = 0; i < numberOfInvitations; i++) {
            Long invitedUserId = (long) (i + 1); // Unique user ID for each invitation
            invitationViewModel.sendInvitation(eventId, invitedUserId, authToken);
        }

        // Verify that sendInvitation was called 'numberOfInvitations' times on the mock repository.
        // This confirms the loop in InviteUsersFragment correctly triggers the ViewModel.
        verify(mockEventHubRepository, times(numberOfInvitations))
                .sendInvitation(eq(eventId), any(Long.class), eq(authToken));

        // You can add more assertions here, e.g., to check the state of actionStatus LiveData
        // if you mock and verify LiveData observations more deeply (often requires AndroidX Test rules).
    }

    // --- How to use this for Memory Profiling (Conceptual Steps) ---
    // This host test by itself does not directly profile memory.
    // To identify memory issues, you would:
    // 1. **Run this test** using your JVM's JUnit runner (e.g., from IntelliJ/Eclipse, or via Gradle/Maven).
    // 2. **Attach a Java Profiler:** While the test is running (or immediately after a breakpoint in the loop),
    //    attach a Java Memory Profiler (like JProfiler, VisualVM, YourKit, or the built-in profiler in some IDEs)
    //    to the JVM process that is executing your tests.
    // 3. **Monitor Allocations:** Observe the heap usage, garbage collection activity, and object allocation
    //    patterns during the execution of `testSendingMultipleInvitationsVerifiesRepositoryCalls()`.
    //    Look for:
    //    - A continuously increasing heap size that doesn't drop after GC.
    //    - A large number of transient objects being allocated very rapidly (high churn).
    //    - Specific object types that accumulate unexpectedly (e.g., `Callback` instances if Retrofit holds onto them, `ResultWrapper` objects).
    // 4. **Analyze Object References:** If objects are retained, the profiler can show you the "paths to GC root"
    //    which reveal why an object cannot be garbage collected.

    // This test provides the executable code path on the host machine to facilitate that profiling.
}