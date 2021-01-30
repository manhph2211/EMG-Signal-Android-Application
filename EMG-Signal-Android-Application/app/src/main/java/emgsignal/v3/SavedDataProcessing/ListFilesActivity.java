/*Show data files saved in external storage*/

package emgsignal.v3.SavedDataProcessing;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import emgsignal.v3.R;
import emgsignal.v3.SignalProcessing.Detrend;
import emgsignal.v3.SignalProcessing.Progressing;


public class ListFilesActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    ArrayList<String> myList;
    ListView listView;
    String nameFolder;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_datafile);
        Intent getNameFolder = getIntent();
        nameFolder = getNameFolder.getStringExtra("NameFolder");
        listView = findViewById(R.id.list_dataFile);
        myList = new ArrayList<>();
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMG_Data/" + nameFolder);
                    if (dir.exists()) {
                        Log.d("path", dir.toString());
                        File[] list = dir.listFiles();
                        Arrays.sort(list, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                        for (int i = 0; i < list.length; i++) {
                            myList.add(list[i].getName());
                        }
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ListFilesActivity.this, android.R.layout.simple_list_item_1, myList);
                        listView.setAdapter(arrayAdapter);
                    }
                } else {
                    requestPermission(); // Code for permission
                }
            } else {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMG_Data/" + nameFolder);

                if (dir.exists()) {
                    Log.d("path", dir.toString());
                    File[] list = dir.listFiles();
                    for (int i = 0; i < list.length; i++) {
                        myList.add(list[i].getName());
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ListFilesActivity.this, android.R.layout.simple_list_item_1, myList);
                    listView.setAdapter(arrayAdapter);
                }
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMG_Data/" + nameFolder + "/" + myList.get(position));
                double[] data = Progressing.readFile(file);;

                for (int i = 0; i < data.length; i++) { ;
                    data[i] = data[i] / 101;
                }
                data = Detrend.detrend(data);

                Log.i("CHECKING LONG", "long data " + data.length);
                Intent intent = new Intent(ListFilesActivity.this, TabView.class);
                intent.putExtra("NameFile", myList.get(position) + "");
                intent.putExtra("TimeData", data);
                intent.putExtra("Length", data.length);
                startActivity(intent);
            }
        });

        //long click delete file
        adapter = new ArrayAdapter<>(ListFilesActivity.this, android.R.layout.simple_list_item_1, myList);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final String item = parent.getItemAtPosition(position).toString();
                //get name of the file want to delete

                new AlertDialog.Builder(ListFilesActivity.this)
                        .setTitle("Do you want to delete this item?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/EMG_Data/" + nameFolder + "/" + myList.get(position);
                                Log.d("pathmother", path);
                                File file = new File(path);
                                boolean deleted = false;
                                if (file.exists()) {
                                    Log.d("ReadFile: ", "file's found");
                                    deleted = file.delete();
                                } else {
                                    Log.d("ReadFileException: ", "cannot find the file");
                                }

                                if (deleted) {
                                    myList.remove(position);
                                    adapter.notifyDataSetChanged();
                                    Toast toastSuccess = Toast.makeText(getApplicationContext(), item + " is deleted", Toast.LENGTH_LONG);
                                    toastSuccess.show();
                                } else {
                                    Toast toastFailed = Toast.makeText(getApplicationContext(), item + " is not deleted, failed", Toast.LENGTH_LONG);
                                    toastFailed.show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });
    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(ListFilesActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(ListFilesActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(ListFilesActivity.this, "Write External Storage permission allows us to read  files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(ListFilesActivity.this, new String[]
                    {android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("value", "Permission Granted, Now you can use local drive .");
            } else {
                Log.e("value", "Permission Denied, You cannot use local drive .");
            }
        }
    }

}

