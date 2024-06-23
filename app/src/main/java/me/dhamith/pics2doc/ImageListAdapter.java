package me.dhamith.pics2doc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ImageViewHolder> {
    private final Context context;

    public ImageListAdapter(Context context) {
        this.context = context;
    }
    @NonNull
    @Override
    public ImageListAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ImageViewHolder(LayoutInflater.from(context).inflate(R.layout.image_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ImageListAdapter.ImageViewHolder holder, int position) {
        Image image = Image.getSelectedImages().get(position);
        holder.imageName.setText(image.getName());
        holder.imagePreview.setImageBitmap(image.getPreview());
        holder.btnRemoveImage.setBackgroundResource(R.drawable.baseline_remove_circle_outline_20);
    }

    @Override
    public int getItemCount() {
        return Image.getSelectedImages().size();
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView imageName;
        private final ImageView imagePreview;
        private final MaterialButton btnRemoveImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);

            imageName = itemView.findViewById(R.id.lblImgName);
            imagePreview = itemView.findViewById(R.id.imgPreview);
            btnRemoveImage = itemView.findViewById(R.id.btnRemoveImage);

            btnRemoveImage.setOnClickListener(view -> {
                int pos = getAdapterPosition();
                Image.getSelectedImages().remove(pos);
                notifyItemRemoved(pos);
            });
        }
    }
}
