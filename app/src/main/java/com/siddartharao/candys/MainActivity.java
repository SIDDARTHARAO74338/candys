package com.siddartharao.candys;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private FrameLayout gameLayout;
    private ImageView basket;
    private TextView scoreText, timerText;
    private int score = 0;
    private int screenWidth, screenHeight;
    private Random random = new Random();
    private boolean isGameOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        gameLayout = findViewById(R.id.frameLayout);
        basket = findViewById(R.id.basket);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);

        // Get screen dimensions
        gameLayout.post(() -> {
            screenWidth = gameLayout.getWidth();
            screenHeight = gameLayout.getHeight();
        });

        // Set basket movement
        basket.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getRawX() - basket.getWidth() / 2f;
                if (x > 0 && x < screenWidth - basket.getWidth()) {
                    basket.setX(x);
                }
            }
            return true;
        });

        startGame();

        timerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.recreate();
            }
        });

    }

    private void startGame() {
        // Start the game timer
        new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                isGameOver = true;
                timerText.setText("Game Over!\n\nrestart game");
            }
        }.start();

        // Generate falling objects
        new Thread(() -> {
            while (!isGameOver) {
                runOnUiThread(this::generateFallingObject);
                try {
                    Thread.sleep(1000); // 1 second interval
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void generateFallingObject() {
        ImageView object = new ImageView(this);
        boolean isBall = random.nextBoolean();
        object.setImageResource(isBall ? R.drawable.candy : R.drawable.bomb);

        int size = 150;
        object.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        int maxX = Math.max(1, screenWidth - size);
        object.setX(random.nextInt(maxX));
        object.setY(0);

        gameLayout.addView(object);

        // Start falling animation
        object.animate()
                .translationY(screenHeight)
                .setDuration(3000)
                .withEndAction(() -> gameLayout.removeView(object)) // Remove after falling
                .start();

        // Continuously check collision
        Handler handler = new Handler();
        Runnable checkCollisionTask = new Runnable() {
            @Override
            public void run() {
                if (checkCollision(object)) {
                    Log.d("Game", "Caught!");
                    if (isBall) {
                        score++;
                    } else {
                        score--;
                    }
                    scoreText.setText("Score: " + score);
                    gameLayout.removeView(object); // Remove object after catching
                } else if (object.getY() < screenHeight) {
                    handler.postDelayed(this, 50); // Check every 50ms
                }
            }
        };
        handler.post(checkCollisionTask);
    }



    private boolean checkCollision(View object) {
        float objectX = object.getX();
        float objectY = object.getY();
        float basketX = basket.getX();
        float basketY = basket.getY();

        // Add a margin for more forgiving collision detection
        int margin = 10;

        return objectX + object.getWidth() - margin > basketX // Object overlaps basket horizontally
                && objectX + margin < basketX + basket.getWidth() // Object overlaps basket horizontally
                && objectY + object.getHeight() - margin > basketY // Object overlaps basket vertically
                && objectY + margin < basketY + basket.getHeight(); // Object overlaps basket vertically
    }

}