package com.memorius.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Renders PDF pages one by one while scrolling (keeps memory low).
class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        final ImageView iv;

        VH(ImageView v) {
            super(v);
            iv = v;
        }
    }

    private final Context ctx;
    private final ParcelFileDescriptor pfd;
    private final PdfRenderer renderer;
    private final int w;
    private final ExecutorService ex = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private boolean closed = false;

    PdfAdapter(Context ctx, String path, int w) throws IOException {
        this.ctx = ctx;
        this.w = w;
        pfd = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
        renderer = new PdfRenderer(pfd);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(ctx);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.bottomMargin = Ui.dp(8);
        iv.setLayoutParams(lp);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackgroundColor(Color.WHITE);
        iv.setMinimumHeight((int) (w * 1.4f));
        return new VH(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull final VH vh, int position) {
        vh.iv.setImageBitmap(null);
        final int pos = position;
        if (closed) return;
        try {
            ex.execute(() -> {
                Bitmap bm = null;
                try {
                    synchronized (renderer) {
                        PdfRenderer.Page pg = renderer.openPage(pos);
                        int h = (int) ((long) pg.getHeight() * w / pg.getWidth());
                        bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        bm.eraseColor(Color.WHITE);
                        pg.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        pg.close();
                    }
                } catch (Exception e) {
                    bm = null;
                }
                final Bitmap fb = bm;
                main.post(() -> {
                    if (fb != null && vh.getBindingAdapterPosition() == pos) vh.iv.setImageBitmap(fb);
                });
            });
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        holder.iv.setImageBitmap(null);
    }

    @Override
    public int getItemCount() {
        return closed ? 0 : renderer.getPageCount();
    }

    void close() {
        if (closed) return;
        closed = true;
        try {
            ex.execute(() -> {
                try {
                    renderer.close();
                    pfd.close();
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
        ex.shutdown();
    }
}
