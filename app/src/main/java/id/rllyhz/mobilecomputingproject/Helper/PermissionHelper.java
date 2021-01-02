package id.rllyhz.mobilecomputingproject.Helper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    public static final int requestCodeOfReadStorage = 100;
    public static final int requestCodeOfRecordAudio = 101;

    public static boolean check(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void checkOrRequest(Context context, Activity activity, String permission, int requestCode) {
        if (check(context, permission)) {
            //showToast(context, "Permission already granted!");
        } else {
            requestPermission(activity, permission, requestCode);
        }
    }

    public static void requestPermission(Activity activity, String permission, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[] {permission}, requestCode);
    }

    private static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private static void showToast(Context context, String message, int length) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showAlert(final Activity activity, final Context context, String title, String message, final String permission, final int requestCode) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Izinkan", null)
                .setNegativeButton("Tidak", null)
                .show();

        Button positiveButton, negativeButton;
        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) alertDialog.dismiss();
                PermissionHelper.requestPermission(activity, permission, requestCode);
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) alertDialog.dismiss();
                showToast(context, "Hak akses ditolak!");
                showToast(context, "Aplikasi mungkin tidak dapat bekerja dengan baik.", Toast.LENGTH_LONG);
            }
        });
    }
}
