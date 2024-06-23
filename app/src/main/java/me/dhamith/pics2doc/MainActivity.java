package me.dhamith.pics2doc;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Dialog progressDialog;
    private ImageListAdapter imageListAdapter;
    private final List<String> blocks = new ArrayList<>();
    private String filename = "";
    private Exception error;
    private int processedImages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageListAdapter = new ImageListAdapter(this);
        RecyclerView recyclerView = findViewById(R.id.imagesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(imageListAdapter);

        ExtendedFloatingActionButton fabAddFiles = findViewById(R.id.fabFileAdd);
        ExtendedFloatingActionButton fabExport = findViewById(R.id.fabExport);

        fabAddFiles.setOnClickListener(view -> {
            Intent data = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            data.setType("image/*");
            data.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            data = Intent.createChooser(data, "Select images");
            filePickerResultLauncher.launch(data);
        });

        fabExport.setOnClickListener(view -> {
            if (Image.getSelectedImages().isEmpty()) {
                return;
            }
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("Export");
            final LinearLayout layout = new LinearLayout(this);
            final TextInputEditText input = new TextInputEditText(this);
            input.setHint("filename");
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            layout.setPadding(20, 20, 20, 20);
            layout.addView(input);
            builder.setView(layout);
            builder.setPositiveButton("Export", (dialog, which) -> {
                if (input.getText() != null && input.getText().length() > 0) {
                    filename = input.getText().toString();
                    progressDialog.show();
                    processImages();
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Processing images");
        final LinearLayout layout = new LinearLayout(this);
        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setPadding(20, 20, 20, 20);
        layout.addView(progressBar);
        builder.setView(layout);
        builder.setCancelable(false);
        progressDialog = builder.create();
    }

    private void processImages() {
        Executor executor = Executors.newSingleThreadExecutor();
        Runnable runnableImageProcessor = () -> {
            for (Image i: Image.getSelectedImages()) {
                InputImage image;
                try {
                    image = InputImage.fromFilePath(MainActivity.this, i.getUri());
                    recognizeText(image);
                } catch (IOException e) {
                    error = e;
                }
            }
        };
        executor.execute(runnableImageProcessor);
    }

    private void recognizeText(InputImage image) {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
        .addOnSuccessListener(visionText -> {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                StringBuilder blockText = new StringBuilder();
                for (Text.Line line : block.getLines()) {
                    blockText.append(" ").append(line.getText());
                }
                blocks.add(blockText.toString());
            }
            processedImages += 1;
            if (processedImages == Image.getSelectedImages().size()) {
                if (error != null) {
                    cleanUpAndNotify(error.getMessage());
                    return;
                }
                createDocx();
            }
        })
        .addOnFailureListener(e -> error = e);
    }

    private void createDocx() {
        XWPFDocument document = new XWPFDocument();
        for (String page : blocks) {
            XWPFParagraph tmpParagraph = document.createParagraph();
            XWPFRun tmpRun = tmpParagraph.createRun();
            tmpRun.setText(page);
            tmpRun.setFontSize(12);
            tmpRun.addBreak();
        }
        try {
            document.write(Files.newOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/"+filename+".docx").toPath()));
            document.close();
            blocks.clear();
            cleanUpAndNotify("Document saved in Downloads");
        } catch (IOException e) {
            cleanUpAndNotify(e.getMessage());
        }
    }

    private void cleanUpAndNotify(String msg) {
        blocks.clear();
        processedImages = 0;
        runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        });
    }

    private final ActivityResultLauncher<Intent> filePickerResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            int count = clipData.getItemCount();
                            for (int i = 0; i < count; i++) {
                                handleSelectedImage(clipData.getItemAt(i).getUri());
                            }
                        } else {
                            handleSelectedImage(data.getData());
                        }
                    }
                }
            }
    );

    private void handleSelectedImage(Uri uri) {
        try {
            Image file = Image.fromUri(this, uri);
            if (!Image.getSelectedImages().contains(file)) {
                Image.getSelectedImages().add(file);
                runOnUiThread(() -> imageListAdapter.notifyItemInserted(Image.getSelectedImages().lastIndexOf(file)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}