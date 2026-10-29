package com.example.coinly.data.ai

import com.example.coinly.R
import com.google.firebase.ai.type.PermissionMissingException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class GeminiErrorsTest {
    @Test fun wrappedNetworkErrorGetsAUsefulMessage() {
        assertEquals(R.string.ai_error_network, GeminiErrors.message(RuntimeException(IOException("offline"))))
    }

    @Test fun missingPermissionIsNotReportedAsASuccess() {
        assertEquals(R.string.ai_error_access, GeminiErrors.message(PermissionMissingException("private server detail")))
    }

    @Test fun unknownServerDetailsAreNotShownToTheUser() {
        assertEquals(R.string.ai_error_unavailable, GeminiErrors.message(IllegalStateException("private server detail")))
    }
}
