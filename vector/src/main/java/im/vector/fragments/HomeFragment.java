/*
 * Copyright 2017 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.rest.callback.ApiCallback;
import org.matrix.androidsdk.rest.callback.SimpleApiCallback;
import org.matrix.androidsdk.rest.model.MatrixError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import im.vector.R;
import im.vector.activity.CommonActivityUtils;
import im.vector.activity.MXCActionBarActivity;
import im.vector.activity.VectorHomeActivity;
import im.vector.activity.VectorRoomActivity;
import im.vector.activity.VectorRoomCreationActivity;

public class HomeFragment extends AbsHomeFragment implements AbsHomeFragment.OnRoomChangedListener {

    /*
     * *********************************************************************************************
     * Static methods
     * *********************************************************************************************
     */

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    /*
     * *********************************************************************************************
     * Fragment lifecycle
     * *********************************************************************************************
     */

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initViews();

        if (savedInstanceState != null) {
            // Restore adapter items
        }
    }

    /*
     * *********************************************************************************************
     * Abstract methods implementation
     * *********************************************************************************************
     */

    @Override
    protected void onFloatingButtonClick() {
        Context context = getActivity();
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        CharSequence items[] = new CharSequence[]{context.getString(R.string.room_recents_start_chat), context.getString(R.string.room_recents_create_room), context.getString(R.string.room_recents_join_room)};
        dialog.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int n) {
                d.cancel();
                if (0 == n) {
                    invitePeopleToNewRoom();
                } else if (1 == n) {
                    createRoom();
                } else {
                    joinARoom();
                }
            }
        });

        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                invitePeopleToNewRoom();
            }
        });

        dialog.setNegativeButton(R.string.cancel, null);
        dialog.show();

    }

    /**
     * Open the room creation with inviting people.
     */
    public void invitePeopleToNewRoom() {
        final Intent settingsIntent = new Intent(getActivity(), VectorRoomCreationActivity.class);
        settingsIntent.putExtra(MXCActionBarActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
        startActivity(settingsIntent);
    }

    /**
     * Offer to join a room by alias or Id
     */
    public void joinARoom() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        View dialogView = inflater.inflate(R.layout.dialog_join_room_by_id, null);
        alertDialogBuilder.setView(dialogView);

        final EditText textInput = (EditText) dialogView.findViewById(R.id.join_room_edit_text);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(R.string.join,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String text = textInput.getText().toString().trim();

                                mSession.joinRoom(text, new ApiCallback<String>() {
                                    @Override
                                    public void onSuccess(String roomId) {
                                        HashMap<String, Object> params = new HashMap<>();
                                        params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
                                        params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                                        CommonActivityUtils.goToRoomPage(getActivity(), mSession, params);
                                    }

                                    private void onError(final String message) {
                                    }

                                    @Override
                                    public void onNetworkError(Exception e) {
                                        onError(e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onMatrixError(final MatrixError e) {
                                        onError(e.getLocalizedMessage());
                                    }

                                    @Override
                                    public void onUnexpectedError(final Exception e) {
                                        onError(e.getLocalizedMessage());
                                    }
                                });
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        final Button joinButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (null != joinButton) {
            joinButton.setEnabled(false);
            textInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String text = textInput.getText().toString().trim();
                    joinButton.setEnabled(MXSession.isRoomId(text) || MXSession.isRoomAlias(text));
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    /**
     * Create a room and open the dedicated activity
     */
    public void createRoom() {
        mSession.createRoom(new SimpleApiCallback<String>(getActivity()) {
            @Override
            public void onSuccess(final String roomId) {
                HashMap<String, Object> params = new HashMap<>();
                params.put(VectorRoomActivity.EXTRA_MATRIX_ID, mSession.getMyUserId());
                params.put(VectorRoomActivity.EXTRA_ROOM_ID, roomId);
                params.put(VectorRoomActivity.EXTRA_EXPAND_ROOM_HEADER, true);
                CommonActivityUtils.goToRoomPage(getActivity(), mSession, params);
            }


            private void onError(final String message) {
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onMatrixError(final MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            @Override
            public void onUnexpectedError(final Exception e) {
                onError(e.getLocalizedMessage());
            }
        });
    }


    @Override
    protected List<Room> getRooms() {
        return new ArrayList<>();
    }

    @Override
    protected void onFilter(String pattern, OnFilterListener listener) {
        Toast.makeText(getActivity(), "home onFilter "+pattern, Toast.LENGTH_SHORT).show();
        //TODO adapter getFilter().filter(pattern, listener)
        //TODO call listener.onFilterDone(); when complete
    }

    @Override
    protected void onResetFilter() {

    }

    /*
     * *********************************************************************************************
     * UI management
     * *********************************************************************************************
     */

    private void initViews() {
        // TODO
    }

    /*
     * *********************************************************************************************
     * Listeners
     * *********************************************************************************************
     */

    @Override
    public void onToggleDirectChat(String roomId, boolean isDirectChat) {

    }

    @Override
    public void onRoomLeft(String roomId) {

    }
}
