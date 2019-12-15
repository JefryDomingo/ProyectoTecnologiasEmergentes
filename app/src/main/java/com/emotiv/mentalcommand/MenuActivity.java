package com.emotiv.mentalcommand;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.emotiv.brainwaves.BrainActivity;
import com.emotiv.raweeg.EegActivity;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends Activity {

    ViewPager viewPager;
    Adapter adapter;
    List<Model> models;
    Integer[] colors = null;
    Button btnOrdr;
    ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        btnOrdr = findViewById(R.id.btnOrder);

        models = new ArrayList<>();
        models.add(new Model(R.drawable.training, "Entrenamiento", "En el Entrenamiento podras controlas tus ondas mentales, mediante cinco técnicas de concentración en un cubo, donde podras moverlo con tu mente y a si podras controlarlo."));
        models.add(new Model(R.drawable.savebw, "Guardar Ondas Cerebrales", "En esta etapa podras guardar tus Ondas Cerebrales, siendo estas 5: Gamma, Beta, Alfa, Theta, Delta, las cuales podras verlas en una archivo y usarlas cuando gustes. "));
        models.add(new Model(R.drawable.saveeeg, "Guardar EEG", "En esta etapa podras guardar los Electroencefalogramas que son data data bruta de los sensores sin la clasificación de las Ondas Cerebrales esto le puede servir más a tu medico especialista"));

        adapter = new Adapter(models, this);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setPadding(130,0,130,0);

        Integer[] colors_temp = {
                getResources().getColor(R.color.colorU),
                getResources().getColor(R.color.colorD),
                getResources().getColor(R.color.colorT)
        };

        colors = colors_temp;

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                if (position < (adapter.getCount()-1) && position < (colors.length -1)) {
                    viewPager.setBackgroundColor(

                            (Integer) argbEvaluator.evaluate(
                                    positionOffset,
                                    colors[position],
                                    colors[position + 1]
                            )
                    );
                } else {
                    viewPager.setBackgroundColor(colors[colors.length - 1]);
                }
            }

            @Override
            public void onPageSelected(final int i) {
                //Toast.makeText(MenuActivity.this,"Pagina selecionada: " + i,Toast.LENGTH_SHORT).show();

                btnOrdr.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (i == 0) {
                            Intent intent = new Intent(MenuActivity.this, ActivityTrainning.class);
                            startActivity(intent);
                        }

                        if (i == 1) {
                            Intent intent = new Intent(MenuActivity.this, BrainActivity.class);
                            startActivity(intent);
                        }

                        if (i == 2) {
                            Intent intent = new Intent(MenuActivity.this, EegActivity.class);
                            startActivity(intent);
                        }
                    }
                });
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
 }

}
