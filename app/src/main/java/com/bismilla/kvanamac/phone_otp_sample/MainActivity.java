package com.bismilla.kvanamac.phone_otp_sample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    String mVerificationId;
    Boolean mVerified = false;
    Timer mTimer;

    Button send_verify_btn;
    TextView timer_tv;
    EditText verification_etv, number_etv;
    ImageView verified_sign_iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        send_verify_btn = findViewById(R.id.send_verify_btn);
        timer_tv = findViewById(R.id.timer_tv);
        verification_etv = findViewById(R.id.verification_etv);
        number_etv = findViewById(R.id.number_etv);
        verified_sign_iv = findViewById(R.id.verified_sign_iv);
        send_verify_btn.setOnClickListener(this);
        timer_tv.setOnClickListener(this);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Log.d("TAG", "onVerificationCompleted:" + phoneAuthCredential);
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w("TAG", "onVerificationFailed", e);
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                } else if (e instanceof FirebaseTooManyRequestsException) {
                }
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.e("TAG", "onCodeSent:" + verificationId);
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("VerificationId", mVerificationId);
                editor.apply();
                mResendToken = forceResendingToken;
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_verify_btn:
                if (send_verify_btn.getTag().equals(getResources().getString(R.string.tag_send))) {
                    if (!number_etv.getText().toString().trim().isEmpty() && number_etv.getText().toString().trim().length() == 10) {
                        String phone = number_etv.getText().toString();
                        startPhoneNumberVerification("+91" + phone);
                        mVerified = false;
                        startTimer();
                        verification_etv.setVisibility(View.VISIBLE);
                        send_verify_btn.setTag(getResources().getString(R.string.tag_verify));
                    } else {
                        number_etv.setError("Please enter valid mobile number");
                    }
                }

                if (send_verify_btn.getTag().equals(getResources().getString(R.string.tag_verify))) {
                    if (!verification_etv.getText().toString().trim().isEmpty() && !mVerified) {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verification_etv.getText().toString().trim());
                        signInWithPhoneAuthCredential(credential);
                    }
                }
                break;
            case R.id.timer_tv:
                if (!number_etv.getText().toString().trim().isEmpty() && number_etv.getText().toString().trim().length() == 10) {
                    String phone = number_etv.getText().toString();
                    reSendVerificationCode(phone, mResendToken);
                    mVerified = false;
                    startTimer();
                    verification_etv.setVisibility(View.VISIBLE);
                    send_verify_btn.setTag(getResources().getString(R.string.tag_verify));
                }
                break;
            default:
                Toast.makeText(getApplicationContext(),"Default method", Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void startTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            int second = 60;

            @Override
            public void run() {
                if (second <= 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timer_tv.setText("RESEND CODE");
                            mTimer.cancel();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timer_tv.setText("00:" + second--);
                        }
                    });
                }
            }
        }, 0, 1000);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            mVerified = true;
                            mTimer.cancel();
                            verified_sign_iv.setVisibility(View.VISIBLE);
                            timer_tv.setVisibility(View.INVISIBLE);
                            number_etv.setEnabled(false);
                            verification_etv.setVisibility(View.INVISIBLE);
                            if (mVerified) {
                                startActivity(new Intent(MainActivity.this, SplashScreenActivity.class));
                                finish();
                            }
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            }
                        }
                    }
                });
    }

    private void reSendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }
}
