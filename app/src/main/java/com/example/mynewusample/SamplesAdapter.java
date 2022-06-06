package com.example.mynewusample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynewusample.model.SampleStructure;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.StringTokenizer;

public class SamplesAdapter extends RecyclerView.Adapter<SamplesAdapter.SamplesViewHolder> {

    Context mContext;
    List<SampleStructure> samplesList;

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public SamplesAdapter(Context mContext, List<SampleStructure> samplesList) {
        this.mContext = mContext;
        this.samplesList = samplesList;
    }

    @NonNull
    @Override
    public SamplesAdapter.SamplesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_sample, parent, false);
        return new SamplesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SamplesAdapter.SamplesViewHolder holder, int position) {
        SampleStructure sample = samplesList.get(position);

        holder.textViewSampleName.setText(sample.getSampleName());
        holder.textViewSampleShortName.setText(makeSampleShortName(sample.getSampleName()));
        if(!sample.getSampleCoverLink().equals("NONE")) {
            try {
                Picasso.get().load(sample.getSampleCoverLink())
                        .error(R.drawable.default_sample_cover_01)
                        .placeholder(R.drawable.default_sample_cover_01)
                        .into(holder.imageViewSampleCover);
                holder.textViewSampleShortName.setVisibility(View.INVISIBLE);
            } catch (Exception e){
                Log.e("SampleList", "Error: " + e.getMessage() + " " + e.getClass().toString());
                holder.textViewSampleShortName.setVisibility(View.VISIBLE);
            }
        }
        else {
            Picasso.get().cancelRequest(holder.imageViewSampleCover);
            holder.imageViewSampleCover.setImageResource(R.drawable.default_sample_cover_01);
            holder.textViewSampleShortName.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return samplesList.size();
    }

    public void removeItem(int position) {
        samplesList.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(SampleStructure item, int position) {
        samplesList.add(position, item);
        notifyItemInserted(position);
    }

    public List<SampleStructure> getData() {
        return samplesList;
    }

    private static String makeSampleShortName(String sampleName){
        StringTokenizer tokens = new StringTokenizer(sampleName);
        String out = "";
        if(tokens.countTokens() > 1){
            String firstWord = tokens.nextToken();
            String secondWord = tokens.nextToken();
            out += Character.toUpperCase(firstWord.charAt(0));
            out += Character.toUpperCase(secondWord.charAt(0));
        } else {
            String firstWord = tokens.nextToken();
            out += Character.toUpperCase(firstWord.charAt(0));
            out += Character.toUpperCase(firstWord.charAt(1));
        }
        return out;
    }

    public static class SamplesViewHolder extends RecyclerView.ViewHolder {

        TextView  textViewSampleName;
        TextView  textViewSampleShortName;
        ImageView imageViewSampleCover;
        ImageView imageViewPlayButton;

        public SamplesViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSampleName = itemView.findViewById(R.id.textViewSampleName);
            textViewSampleShortName = itemView.findViewById(R.id.textViewSampleShortName);
            imageViewSampleCover = itemView.findViewById(R.id.imageViewSampleCover);
            imageViewPlayButton = itemView.findViewById(R.id.imageViewPlayButton);
            Drawable pause = itemView.getContext().getDrawable(R.drawable.ic_round_pause_24);
            Drawable play = itemView.getContext().getDrawable(R.drawable.ic_round_play_arrow_24);
            imageViewPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(imageViewPlayButton.getDrawable().equals(pause)){
                        imageViewPlayButton.setImageDrawable(play);
                    } else {
                        imageViewPlayButton.setImageDrawable(pause);
                    }
                }
            });
        }
    }
}
