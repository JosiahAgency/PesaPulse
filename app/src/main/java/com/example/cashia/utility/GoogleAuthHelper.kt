package com.example.cashia.utility

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private val auth = FirebaseAuth.getInstance()

    suspend fun signInWithGoogle(): String? = withContext(Dispatchers.IO) {
        // NOTE: In a real app, you would use your Web Client ID from the Google Cloud Console.
        // For this implementation, we use a placeholder if one isn't provided.
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("803805594822-0q78dbmb9ea55b50advhommjojrs4d5m.apps.googleusercontent.com")
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthHelper", "Credential error: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("GoogleAuthHelper", "Unexpected error: ${e.message}")
            null
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): String? {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val idToken = credential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            return authResult.user?.email
        } else {
            Log.e("GoogleAuthHelper", "Unexpected credential type")
            return null
        }
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("GoogleAuthHelper", "Sign out error: ${e.message}")
        }
    }
}
