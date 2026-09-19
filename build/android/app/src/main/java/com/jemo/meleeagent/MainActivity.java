package com.jemo.meleeagent;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

/**
 * Thin UI over the melee-decomp Python CLI.
 *
 * The user types CLI arguments (e.g. "--help"), taps Run, and the output of
 * Python's run_command(args) is shown in the scrollable TextView.
 *
 * Python runs on a background thread: interpreter startup and CLI execution
 * can take seconds, and must never block the UI thread (ANR). Chaquopy is
 * thread-safe, so this is fine.
 */
public class MainActivity extends Activity {

    private EditText argsInput;
    private Button runButton;
    private TextView outputView;
    private ScrollView outputScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        argsInput = findViewById(R.id.args_input);
        runButton = findViewById(R.id.run_button);
        outputView = findViewById(R.id.output_view);
        outputScroll = findViewById(R.id.output_scroll);

        runButton.setOnClickListener(v -> runCommand());
    }

    private void runCommand() {
        final String args = argsInput.getText().toString();
        runButton.setEnabled(false);
        outputView.setText("Running: melee-decomp " + args + "\n\n…");

        new Thread(() -> {
            String result;
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(MainActivity.this));
                }
                // Calls main.py: def run_command(args_str: str) -> str
                // Chaquopy converts the Java String to a Python str argument
                // automatically, and PyObject.toString() on a Python str
                // returns its contents as a Java String.
                PyObject pyResult = Python.getInstance()
                        .getModule("main")
                        .callAttr("run_command", args);
                result = (pyResult == null) ? "<no output>" : pyResult.toString();
            } catch (Exception e) {
                // Python-side failures are already captured into the output
                // string by main.py; this branch is for Java-side failures
                // (e.g. Python failing to start).
                result = "Java-side error: " + e + "\n";
            }

            // final copy: lambdas can only capture final/effectively-final locals
            final String output = result;
            runOnUiThread(() -> {
                outputView.setText(output);
                runButton.setEnabled(true);
                outputScroll.post(() -> outputScroll.fullScroll(View.FOCUS_UP));
            });
        }, "melee-decomp-runner").start();
    }
}
