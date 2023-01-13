package xtr.keymapper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import xtr.keymapper.activity.MainActivity;
import xtr.keymapper.server.InputService;

public class Server {

    private final Context context;
    public final String script_name;

    public static final int MAX_LINES = 16;

    private int counter1 = 0;

    public Server(Context context){
        this.context = context;
        script_name = context.getExternalFilesDir(null) + "/xtMapper.sh";
    }

    private void writeScript(ApplicationInfo ai) throws IOException, InterruptedException {
        final String className = InputService.class.getName();

        FileWriter linesToWrite = new FileWriter(script_name);
        linesToWrite.append("#!/system/bin/sh\n");
        linesToWrite.append("pgrep -f ").append(className).append(" && echo Waiting for overlay... && exit 1\n");
        linesToWrite.append("exec env ");
        linesToWrite.append("LD_LIBRARY_PATH=\"").append(ai.nativeLibraryDir)  //path containing lib*.so
                .append("\" CLASSPATH=\"").append(ai.publicSourceDir) // Absolute path to apk in /data/app
                .append("\" /system/bin/app_process /system/bin ")
                .append(className).append("\n");

        linesToWrite.flush();
        linesToWrite.close();
    }

    public void setupServer () {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            writeScript(ai);
        } catch (IOException | InterruptedException | PackageManager.NameNotFoundException e) {
            Log.e("Server", e.toString());
        }
    }

    public void startServer() {
        updateCmdView1("exec sh " + script_name);
        try {
            Process sh = Utils.getRootAccess();
            DataOutputStream outputStream = new DataOutputStream(sh.getOutputStream());
            outputStream.writeBytes("/system/bin/sh " + script_name);
            outputStream.close();

            BufferedReader stdout = new BufferedReader(new InputStreamReader(sh.getInputStream()));
            String line;
            while ((line = stdout.readLine()) != null) {
                updateCmdView1("stdout: " + line);
                if (line.equals("Waiting for overlay...")) {
                    ((MainActivity)context).startPointer();
                }
            }
            sh.waitFor();
        } catch (IOException | InterruptedException e) {
            Log.e("Server", e.toString());
        }
    }

    public void updateCmdView1(String s){
        if(counter1 < MAX_LINES) {
            ((MainActivity)context).c1.append(s).append("\n");
            counter1++;
        } else {
            counter1 = 0;
            ((MainActivity)context).c1 = new StringBuilder();
        }
    }
}
