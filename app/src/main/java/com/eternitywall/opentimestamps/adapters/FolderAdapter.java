package com.eternitywall.opentimestamps.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.eternitywall.opentimestamps.IOUtil;
import com.eternitywall.opentimestamps.R;
import com.eternitywall.opentimestamps.models.Folder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    List<Folder> mDataset = new ArrayList<>();
    OnItemClickListener mItemClickListener;
    Context mContext;

// Provide a reference to the views for each data item
// Complex data items may need more than one view per item, and
// you provide access to all the views for a data item in a view holder

    public class ViewHolder extends RecyclerView.ViewHolder{
        // each data item is just a string in this case
        public TextView tvTitle,tvSubtitle;
        public ImageView ivStatus;
        public Switch swEnabled;
        public ViewHolder(View v) {
            super(v);
            tvTitle = (TextView) v.findViewById(R.id.tvTitle);
            tvSubtitle = (TextView) v.findViewById(R.id.tvSubtitle);
            swEnabled = (Switch) v.findViewById(R.id.swEnabled);
            ivStatus = (ImageView) v.findViewById(R.id.ivStatus);

            tvTitle.setText("");
            tvSubtitle.setText("");

            // click on switch
            swEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mItemClickListener == null) {
                        return;
                    }
                    int position = getAdapterPosition();
                    if (isChecked){
                        mItemClickListener.onEnableClick(itemView, position, mDataset.get(position).id);
                    } else {
                        mItemClickListener.onDisableClick(itemView, position, mDataset.get(position).id);
                    }
                }
            });
            // click on status imageview
            ivStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener == null) {
                        return;
                    }
                    int position = getAdapterPosition();
                    mItemClickListener.onCheckingClick(itemView, position, mDataset.get(position).id);
                }
            });
            // click on text
            View.OnClickListener textOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener == null) {
                        return;
                    }
                    int position = getAdapterPosition();
                    mItemClickListener.onDetailClick(itemView, position, mDataset.get(position).id);
                }
            };
            tvTitle.setOnClickListener(textOnClickListener);
            tvSubtitle.setOnClickListener(textOnClickListener);
        }
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public FolderAdapter(Context context, List<Folder> myDataset) {
        mContext = context;
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FolderAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        synchronized (mDataset) {
            Folder folder = mDataset.get(position);

            holder.tvTitle.setText(folder.name);
            holder.swEnabled.setChecked(folder.enabled);

            if (folder.state == Folder.State.CHECKING){
                holder.tvSubtitle.setText(String.valueOf(folder.countFiles)+mContext.getString(R.string.new_changed_files_found));
            } else if (folder.state == Folder.State.STAMPING) {
                holder.tvSubtitle.setText(String.valueOf(folder.countFiles)+mContext.getString(R.string.storing_proof_files));
            } else if (folder.state == Folder.State.EXPORTING) {
                String filepath = folder.zipPath(holder.itemView.getContext());
                String filename = filepath.substring( filepath.lastIndexOf("/") );
                holder.tvSubtitle.setText(String.valueOf(folder.countFiles)+mContext.getString(R.string.exporting_proof_files_at) + filename);
            } else if (folder.state == Folder.State.NOTHING){
                holder.tvSubtitle.setText("Never timestamped");
            } else if (folder.state == Folder.State.STAMPED){
                holder.tvSubtitle.setText("Last timestamp at\n"+IOUtil.getDate(folder.lastSync,"dd/MM/yyyy hh:mm"));
            } else if (folder.state == Folder.State.NOTUPDATED){
                holder.tvSubtitle.setText(String.valueOf(folder.countFiles)+mContext.getString(R.string.new_changed_files_since)+IOUtil.getDate(folder.lastSync,"dd/MM/yyyy hh:mm"));
            } else if (folder.state == Folder.State.EXPORTED){
                holder.tvSubtitle.setText(folder.zipPath(holder.itemView.getContext()));
            }

            Drawable drawable = null;
            if (folder.state == Folder.State.NOTHING) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_attention).mutate();
            } else if (folder.state == Folder.State.NOTUPDATED) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_updating).mutate();
            } else if (folder.state == Folder.State.STAMPED) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_timestamped).mutate();
            } else if (folder.state == Folder.State.STAMPING) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_timestamping).mutate();
            } else if (folder.state == Folder.State.CHECKING) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_updating).mutate();
            } else if (folder.state == Folder.State.EXPORTING) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_exporting).mutate();
            } else if (folder.state == Folder.State.EXPORTED) {
                drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_exporting).mutate();
            }
            holder.ivStatus.setImageDrawable(drawable);

        }

    }

    public interface OnItemClickListener {
        public void onDetailClick(View view, int position, long id);
        public void onCheckingClick(View view, int position, long id);
        public void onEnableClick(View view, int position, long id);
        public void onDisableClick(View view, int position, long id);
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}