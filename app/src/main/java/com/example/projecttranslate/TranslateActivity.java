package com.example.projecttranslate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class TranslateActivity extends AppCompatActivity {


    private TextView mSourceLang;
    private EditText mSourcetext;
    private Button mTranslateBtn;
    private TextView mTranslatedText;
    private String sourceText;
    private Spinner mLanguageSelector;
    private String targetCode;
    private List<String> language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        targetCode = TranslateLanguage.ENGLISH;
        mSourceLang = findViewById(R.id.sourceLang);
        mSourcetext = findViewById(R.id.sourceText);
        mTranslateBtn = findViewById(R.id.translate);
        mTranslatedText = findViewById(R.id.translatedText);
        mLanguageSelector = findViewById(R.id.langSelector);
        //language = new ArrayList<>(TranslateLanguage.getAllLanguages().size());
        language = TranslateLanguage.getAllLanguages();

        mSourcetext.setText(getIntent().getExtras().getString("Value"));

        if(isNetworkAvailable()) {
            Log.d("Language Identify", "Internet access available");
            mSourceLang.setText("All models available, first time translation to a new language will download the model");

        } else {
            Log.d("Language Identify", "No Internet");
            RemoteModelManager modelManager = RemoteModelManager.getInstance();
            // Get translation models stored on the device.

            modelManager.getDownloadedModels(TranslateRemoteModel.class)
                    .addOnSuccessListener(new OnSuccessListener<Set<TranslateRemoteModel>>() {
                        @Override
                        public void onSuccess(Set<TranslateRemoteModel> models) {
                            // ...

                            List<String> finalLanguages = new ArrayList<>(models.size());
                            for(TranslateRemoteModel model: models) {
                                //Log.d("Language Models", "Models: " + model.getLanguage());
                                finalLanguages.add(model.getLanguage());
                            }
                            mSourceLang.setText("Available offline models: " + finalLanguages.toString());
                            Log.d("Language Models", "Available offline Models: " + finalLanguages.toString());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Error.
                        }
                    });
        }



       // Log.d("Language ", "Final spinner Models: " + language.toString());
        final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, language);

        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLanguageSelector.setAdapter(aa);
        mLanguageSelector.setSelection(12);
        mLanguageSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                targetCode = TranslateLanguage.fromLanguageTag(aa.getItem(i));
                Log.d("Language Identify", "Spinner Language: " + aa.getItem(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mTranslateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                identifyLanguage();
            }
        });


    }

    private void identifyLanguage() {
        sourceText = mSourcetext.getText().toString();

        if(sourceText.isEmpty()) {
            Toast.makeText(getApplicationContext(),"Please enter text to be translated", Toast.LENGTH_SHORT).show();
        } else {

            LanguageIdentifier languageIdentifier =
                    LanguageIdentification.getClient();
            languageIdentifier.identifyLanguage(sourceText)
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@Nullable String languageCode) {
                                    if (languageCode.equals("und")) {
                                        Log.d("Language Identify", "Can't identify language.");
                                        Toast.makeText(getApplicationContext(), "Language Not Identified", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.d("Language Identify", "Language: " + languageCode);

                                        translateText(languageCode);

                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Model couldn’t be loaded or other internal error.
                                    // ...
                                }
                            });
        }
    }

    private void translateText(String langCode) {
        mTranslatedText.setText("Translating..");

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(langCode)
                        .setTargetLanguage(targetCode)
                        .build();


        final Translator translator =
                Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                // Model downloaded successfully. Okay to start translating.
                                // (Set a flag, unhide the translation UI, etc.)
                                translator.translate(sourceText)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(@NonNull String s) {
                                                        // Translation successful.
                                                        Log.d("Language Translation", "Translation: " + s);
                                                        mTranslatedText.setText(s);
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Error.
                                                        // ...

                                                    }
                                                });

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                                Log.d("Language Translation", "Model not found ");
                            }
                        });

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }

}
