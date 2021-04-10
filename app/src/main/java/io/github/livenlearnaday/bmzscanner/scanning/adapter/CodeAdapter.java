package io.github.livenlearnaday.bmzscanner.scanning.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.livenlearnaday.bmzscanner.R;
import io.github.livenlearnaday.bmzscanner.data.entity.CodeDetail;
import timber.log.Timber;


public class CodeAdapter extends ListAdapter<CodeDetail, CodeAdapter.ViewHolder> {


    public CodeAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<CodeDetail> DIFF_CALLBACK = new DiffUtil.ItemCallback<CodeDetail>() {
        @Override
        public boolean areItemsTheSame(@NonNull CodeDetail oldItem, @NonNull CodeDetail newItem) {
            return oldItem.getId() == newItem.getId();

        }

        @Override
        public boolean areContentsTheSame(@NonNull CodeDetail oldItem, @NonNull CodeDetail newItem) {
            return oldItem.getCodeString().equals(newItem.getCodeString()) &&
                    oldItem.getCodeLength().equals(newItem.getCodeLength());

        }
    };


    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.qr_text_view)
        TextView qrTextView;
         @BindView(R.id.code_length_text_view)
         TextView codeLengthTextView;

        public ViewHolder(View itemView) {
             super(itemView);
             ButterKnife.bind(this, itemView);

        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {


        Timber.v( String.format("onCreateViewHolder %25s  ", getItem(position).getCodeString()));


        View rowView = LayoutInflater.from(parent.getContext()).inflate(R.layout.qr_item, parent, false);

        return new ViewHolder(rowView);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        CodeDetail currentCodeDetail = getItem(position);

        Log.v("Logging", String.format("onBindViewHolder %25s ", currentCodeDetail.getCodeString()));

        holder.qrTextView.setText(currentCodeDetail.getCodeString());
        holder.codeLengthTextView.setText(currentCodeDetail.getCodeLength());



    }



   public CodeDetail getCodeDetailAt(int position){
        return getItem(position);


   }




}
