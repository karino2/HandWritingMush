package com.livejournal.karino2.handwritingmash.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends ActionBarActivity {

    static final int DIALOG_ID_URL = 1;
    static final int ACTIVITY_ID_SEND = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String someString = getIntent().getStringExtra("replace_key");
        shouldCopyBackUrl = !someString.equals("");
    }

    boolean shouldCopyBackUrl = false;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    private Uri putFileToContentDB(File file) {
        ContentResolver resolver = getBaseContext().getContentResolver();

        ContentValues content = new ContentValues(4);

        content.put(MediaStore.Images.ImageColumns.TITLE, "Tegaki");
        content.put(MediaStore.Images.ImageColumns.DATE_ADDED,
                System.currentTimeMillis() / 1000);
        content.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        content.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content);
    }


    private File saveBitmap(Bitmap screen) throws IOException {
        File dir = getFileStoreDirectory();
        File result = new File(dir, newFileName());

        OutputStream stream = new FileOutputStream(result);
        screen.compress(Bitmap.CompressFormat.PNG, 80, stream);
        stream.close();
        return result;
    }

    public static String newFileName() {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        String filename = timeStampFormat.format(new Date()) + ".png";
        return filename;
    }

    public static void ensureDirExist(File dir) throws IOException {
        if(!dir.exists()) {
            if(!dir.mkdir()){
                throw new IOException();
            }
        }
    }

    public static File getFileStoreDirectory() throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "HandwringMash");
        ensureDirExist(dir);
        return dir;
    }


    private void deleteAllFiles(File folder) {
        for(File file : folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if(pathname.isDirectory())
                    return false;
                return true;
            }
        })) {
            ContentResolver resolver = getBaseContext().getContentResolver();
            resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    "? = ?",
                    new String[]{MediaStore.Images.ImageColumns.DATA,
                            file.getAbsolutePath()}
            );

            file.delete();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(shouldCopyBackUrl)
            copyBackUrl();
    }

    private void copyBackUrl() {
        Intent data = new Intent();
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager.getPrimaryClip().getItemCount() == 0) {
            showMessage("no clipboard data");
            return; // do nothing.
        }
        ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
        if(item.getText() == null) {
            showMessage("clipboard is not text");
            return; // do nothing.
        }
        String result = item.getText().toString();

        data.putExtra("replace_key", result);
        setResult(RESULT_OK, data);
        finish();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            DrawingCanvas dc = getDrawingCanvas();
            Bitmap bitmap = dc.getBitmap();
            File file = null;
            try {
                deleteAllFiles(getFileStoreDirectory());
                file = saveBitmap(bitmap);
            } catch (IOException e) {
                showMessage("save fail: " + e.getMessage());
                return true;
            }
            Uri uri = putFileToContentDB(file);


            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if(shouldCopyBackUrl) {
                startActivityForResult(Intent.createChooser(intent, "Share Image"), ACTIVITY_ID_SEND);
            }
            else {
                startActivity(Intent.createChooser(intent, "Share Image"));
                finish();
            }

            return true;
        }
        if (id== R.id.action_clear) {
            DrawingCanvas dc = getDrawingCanvas();
            dc.resetCanvas();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            getDrawingCanvas().undo();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        {
            getDrawingCanvas().redo();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private DrawingCanvas getDrawingCanvas() {
        return (DrawingCanvas)findViewById(R.id.canvas);
    }

}
