
package info.guardianproject.trustedintents.test;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.AndroidIncludedAppsPin;
import com.android.AndroidSystemPin;

import info.guardianproject.trustedintents.TrustedIntents;

public class TrustedIntentsTests extends AndroidTestCase {
    private static final String TAG = "TrustedIntentsTests";

    Context context;
    PackageManager pm;
    final String[] packagesSignedByAndroidIncludedApps = new String[] {
            "com.android.browser", "com.android.calculator2", "com.android.calendar",
            "com.android.dreams.basic", "com.android.providers.calendar",
            "com.android.camera", "com.android.deskclock", "com.android.gesture.builder",
            "com.android.smoketest", "com.android.smoketest.tests",
            "com.android.emulator.connectivity.test", "com.android.development_settings",
            "com.android.email", "com.example.android.livecubes", "com.android.exchange"
    };
    final String[] packagesSignedByAndroidSystem = new String[] {
            "android", "com.android.certinstaller", "com.android.backupconfirm",
            "com.android.keyguard", "com.android.sdksetup", "com.android.sharedstoragebackup",
            "com.android.customlocale2", "com.android.development", "com.android.documentsui",
            "com.android.externalstorage", "com.android.location.fused", "com.android.inputdevices"
    };

    @Override
    public void setUp() {
        context = getContext();
        pm = context.getPackageManager();
    }

    private void checkAreSignaturesEqual(String[] packages) {
        Signature[] first = null;
        Signature[] second = null;
        for (int i = 0; i < packages.length; i++) {
            try {
                PackageInfo pkgInfo = pm.getPackageInfo(packages[i], PackageManager.GET_SIGNATURES);
                first = pkgInfo.signatures;
            } catch (NameNotFoundException e) {
                Log.w(TAG, "NameNotFoundException: " + e.getMessage());
                continue;
            }
            for (int j = 0; j < packages.length; j++) {
                if (i == j)
                    continue;
                try {
                    PackageInfo pkgInfo = pm.getPackageInfo(packages[j],
                            PackageManager.GET_SIGNATURES);
                    second = pkgInfo.signatures;
                } catch (NameNotFoundException e) {
                    Log.w(TAG, "NameNotFoundException: " + e.getMessage());
                    continue;
                }
                assertTrue(TrustedIntents.get(context).areSignaturesEqual(first, second));
            }
        }
    }

    public void testCheckAreSignaturesEqual() {
        checkAreSignaturesEqual(packagesSignedByAndroidIncludedApps);
        checkAreSignaturesEqual(packagesSignedByAndroidSystem);
    }

    public void testCheckAreSignaturesNotEqual() {
        assertFalse(TrustedIntents.get(context).areSignaturesEqual(
                new AndroidIncludedAppsPin().getSignatures(),
                new AndroidSystemPin().getSignatures()));
        PackageInfo pkgInfo;
        Signature[] first = null;
        Signature[] second = null;
        int length = packagesSignedByAndroidSystem.length;
        if (length > packagesSignedByAndroidIncludedApps.length)
            length = packagesSignedByAndroidIncludedApps.length;
        for (int i = 0; i < length; i++) {
            try {
                pkgInfo = pm.getPackageInfo(
                        packagesSignedByAndroidSystem[i],
                        PackageManager.GET_SIGNATURES);
                first = pkgInfo.signatures;
                pkgInfo = pm.getPackageInfo(
                        packagesSignedByAndroidIncludedApps[i],
                        PackageManager.GET_SIGNATURES);
                second = pkgInfo.signatures;
            } catch (NameNotFoundException e) {
                Log.w(TAG, "NameNotFoundException: " + e.getMessage());
                continue;
            }
            assertFalse(TrustedIntents.get(context).areSignaturesEqual(first, second));
        }
    }

    public void testCheckPin() {
        Intent intent;
        TrustedIntents ti = TrustedIntents.get(context);

        assertFalse(ti.isReceiverTrusted(new Intent()));
        intent = new Intent();
        intent.setPackage("");
        assertFalse(ti.isReceiverTrusted(intent));

        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }

        ti.addTrustedSigner(new AndroidSystemPin());
        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertFalse(ti.isReceiverTrusted(intent));
        }

        ti.addTrustedSigner(new AndroidIncludedAppsPin());
        for (String packageName : packagesSignedByAndroidSystem) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
        for (String packageName : packagesSignedByAndroidIncludedApps) {
            intent = new Intent();
            intent.setPackage(packageName);
            assertTrue(ti.isReceiverTrusted(intent));
        }
    }
}
