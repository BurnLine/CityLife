package com.burnline.citylife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static final int THREAD_DRAW = 1;
    private static final int THREAD_STOP = 2;
    private static final String TAG = MainSurface.class.getSimpleName();

    private MainThread thread;
    private final BlockingQueue<Integer> blocking;

    public MainSurface(Context context) {
        super(context);
        getHolder().addCallback(this);
        blocking = new LinkedBlockingQueue<>(1);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        setWillNotDraw(false); // Allows us to use invalidate() to call onDraw()
        thread = new MainThread(); // Start the thread that will make calls to onDraw()
        thread.start();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Surface created");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        redraw();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Surface have now resolution: " + width + "x" + height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        try {
            blocking.put(THREAD_STOP);
            thread.join(); // Removes thread from memory
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Surface destroyed");
        }
    }

    private void redraw() {
        blocking.offer(THREAD_DRAW); // insert if queue is free
    }

    class MainThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private final String TAG = MainThread.class.getSimpleName();

        MainThread() {
            this.surfaceHolder = getHolder();
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Started drawing loop");
            }

            while (true) {
                Canvas c = null;

                try {
                    if (MainSurface.this.blocking.take() == THREAD_STOP) {
                        break;
                    }

                    c = surfaceHolder.lockCanvas();

                    synchronized(surfaceHolder) {
                        if (c != null) {
                            c.drawColor(Color.WHITE);
                            // TODO draw
                        }

                        postInvalidate();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Finished drawing loop");
            }
        }
    }
}
