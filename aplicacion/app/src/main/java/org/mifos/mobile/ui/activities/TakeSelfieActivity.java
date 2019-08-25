package org.mifos.mobile.ui.activities;

import android.os.Bundle;

import org.mifos.mobile.R;
import org.mifos.mobile.ui.activities.base.BaseActivity;
import org.mifos.mobile.ui.activities.AddPersonPreviewActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.util.Date;

import java.io.File;

import ch.zhaw.facerecognitionlibrary.Helpers.FileHelper;

public class TakeSelfieActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);
        //EditText txt_Name = (EditText)findViewById(R.id.et_email);

        String name = "selfservice33";//+Math.random();//txt_Name.getText().toString();
        Intent intent = new Intent(this, AddPersonPreviewActivity.class);
        intent.putExtra("Name", name);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //if(btnTimeManually.isChecked()){
        //  intent.putExtra("Method", AddPersonPreviewActivity.MANUALLY);
        //} else {
            intent.putExtra("Method", AddPersonPreviewActivity.TIME);
        //}

        //if(btnTrainingTest.isChecked()){
        // Add photos to "Test" folder
        if(isNameAlreadyUsed(new FileHelper().getTestList(), name)){
            Toast.makeText(getApplicationContext(), "El usuario ya esta registrado.", Toast.LENGTH_SHORT).show();
        } else {
            intent.putExtra("Folder", "Test");
            //if(btnReferenceDeviation.isChecked()){
            intent.putExtra("Subfolder", "deviation");
        //} else {
        //  intent.putExtra("Subfolder", "reference");
        //}
            startActivity(intent);
        }
        /*} else {
               // Add photos to "Training" folder
        if(isNameAlreadyUsed(new FileHelper().getTrainingList(), name)){
        Toast.makeText(getApplicationContext(), "This name is already used. Please choose another one.", Toast.LENGTH_SHORT).show();
        } else {
            intent.putExtra("Folder", "Training");
        startActivity(intent);
        }
        }*/
        //}
        //});
    }

    private boolean isNameAlreadyUsed(File[] list, String name){
        boolean used = false;
        if(list != null && list.length > 0){
            for(File person : list){
                // The last token is the name --> Folder name = Person name
                String[] tokens = person.getAbsolutePath().split("/");
                final String foldername = tokens[tokens.length-1];
                if(foldername.equals(name)){
                    used = true;
                    break;
                }
            }
        }
        return used;
    }
}
