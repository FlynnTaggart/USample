package com.example.mynewusample;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynewusample.model.SampleStructure;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class SamplesAdapter extends RecyclerView.Adapter<SamplesAdapter.SamplesViewHolder> implements Filterable {

    private Context mContext;
    private List<SampleStructure> samplesList;
    private List<SampleStructure> samplesListFull;
    private Filter samplesFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<SampleStructure> filteredList = new ArrayList<>();

            if(constraint == null || constraint.length() == 0){
                filteredList.addAll(samplesListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for(SampleStructure i : samplesListFull){
                    if(i.getSampleName().toLowerCase().contains(filterPattern)){
                        filteredList.add(i);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            samplesList.clear();
            samplesList.addAll((List) filterResults.values);
            notifyDataSetChanged();
        }
    };

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public SamplesAdapter(Context mContext, List<SampleStructure> samplesList) {
        this.mContext = mContext;
        this.samplesList = samplesList;
        this.samplesListFull = new ArrayList<>(samplesList);
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

    public void updateFullList(){
        this.samplesListFull = new ArrayList<>(samplesList);
    }

    public int removeItem(int position) {
        int fullPosition = samplesListFull.indexOf(samplesList.get(position));
        samplesListFull.remove(samplesList.get(position));
        samplesList.remove(position);
        notifyItemRemoved(position);
        return fullPosition;
    }

    public void restoreItem(SampleStructure item, int position, int fullPosition) {
        samplesListFull.add(fullPosition, item);
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

    @Override
    public Filter getFilter() {
        return samplesFilter;
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
