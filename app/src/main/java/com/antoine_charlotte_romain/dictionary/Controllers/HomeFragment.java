package com.antoine_charlotte_romain.dictionary.Controllers;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.antoine_charlotte_romain.dictionary.Business.Dictionary;
import com.antoine_charlotte_romain.dictionary.Controllers.Lib.HeaderGridView;
import com.antoine_charlotte_romain.dictionary.DataModel.DictionaryDataModel;
import com.antoine_charlotte_romain.dictionary.R;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements DictionaryAdapter.DictionaryAdapterCallback {


    /*---------------------------------------------------------
    *                        CONSTANTS
    *---------------------------------------------------------*/

    private final int CONTEXT_MENU_READ = 0;
    private final int CONTEXT_MENU_UPDATE = 1;
    private final int CONTEXT_MENU_DELETE = 2;
    private final int NORMAL_STATE = 0;
    private final int DELETE_STATE = 1;

    /*---------------------------------------------------------
    *                     INSTANCE VARIABLES
    *---------------------------------------------------------*/

    /**
     * The view corresponding to this fragment.
     *
     * @see MainActivity
     */
    private View v;

    /**
     * Initial dictionary list. Contains all the dictionary.
     */
    private ArrayList<Dictionary> dictionaries;

    /**
     * List of displayed dictionaries according to the research performed
     */
    private ArrayList<Dictionary> dictionariesDisplay;

    /**
     * List of dictionaries to delete
     */
    private ArrayList<Dictionary> deleteList;

    /**
     * Allow to display a list of Objects.
     */
    private HeaderGridView gridView;

    /**
     * Custom ArrayAdapter to manage the different rows of the grid
     */
    private DictionaryAdapter adapter;

    /**
     * Used to display the snackBars
     */
    private CoordinatorLayout rootLayout;

    /**
     * Used to communicating with the database
     */
    private DictionaryDataModel ddm;

    /**
     * Used to handle a undo action after deleting a dictionary
     */
    private boolean undo;

    /**
     * Header of the gridView
     */
    private View header;

    private int state;

    /**
     * Toolbar menu
     */
    private Menu menu;

    private EditText searchBox, nameBox;


    /*---------------------------------------------------------
    *                       CONSTRUCTORS
    *---------------------------------------------------------*/

    public HomeFragment() {
        // Required empty public constructor
    }



    /*---------------------------------------------------------
    *                     INSTANCE METHODS
    *---------------------------------------------------------*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        v = inflater.inflate(R.layout.fragment_home,container,false);
        rootLayout = (CoordinatorLayout) v.findViewById(R.id.rootLayout);
        setHasOptionsMenu(true);
        state = NORMAL_STATE;

        initData();
        initGridView();
        initFloatingActionButton();
        initEditText();
        return v;
    }


    /**
     * Initialising the data model and selecting all the dictionaries
     */
    private void initData()
    {
        ddm = new DictionaryDataModel(getActivity());
        ddm.open();
        dictionaries = ddm.selectAll();
        dictionariesDisplay = new ArrayList<>(dictionaries);
    }

    /**
     * Initialising the GridView to display the dictionary list and making its clickables
     */
    private void initGridView()
    {
        //Creating the GridView
        gridView = (HeaderGridView) v.findViewById(R.id.dictionary_list);
        gridView.setDrawSelectorOnTop(true);

        if(state == NORMAL_STATE)
        {
            //Adding the GridView header
            gridView.removeHeaderView(header);
            header = getActivity().getLayoutInflater().inflate(R.layout.grid_view_header, null);
            gridView.addHeaderView(header);
            Button b = (Button) header.findViewById(R.id.button_all);
            b.setText(R.string.all_dictionaries);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    read(-1);
                }
            });

            //Configuring the ListView listener
            gridView.setOnItemClickListener(
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            read(position - 1);
                        }
                    }
            );

            //Populating the GridView
            adapter = new DictionaryAdapter(getActivity(), R.layout.dictionary_row, dictionariesDisplay);
            adapter.setCallback(this);
            gridView.setAdapter(adapter);
        }
        else if(state == DELETE_STATE)
        {
            //Adding the GridView header
            gridView.removeHeaderView(header);
            header = getActivity().getLayoutInflater().inflate(R.layout.grid_view_header, null);
            gridView.addHeaderView(header);
            Button b = (Button) header.findViewById(R.id.button_all);
            b.setText(R.string.select_all);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for ( int i=0; i < adapter.getCount(); i++) {

                    }
                }
            });

            adapter = new DictionaryAdapter(getActivity(), R.layout.delete_dictionary_row, dictionariesDisplay);
            adapter.setCallback(this);
            gridView.setAdapter(adapter);
            deleteList = new ArrayList<Dictionary>();
        }

        //Adding the context menu on each rows
        registerForContextMenu(gridView);

        Animation anim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
        gridView.setAnimation(anim);
        anim.start();

    }


    /**
     * Initialising the search box to dynamically researching on the dictionary list
     */
    private void initEditText()
    {
        //Creating the EditText for searching inside the dictionaries list
        searchBox = (EditText) v.findViewById(R.id.search_field);
        searchBox.setMovementMethod(new ScrollingMovementMethod());
        searchBox.setText("");
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dictionariesDisplay.clear();
                String search = s.toString();
                for (int i = 0; i < dictionaries.size(); i++) {
                    if (dictionaries.get(i).getTitle().toLowerCase().contains(search.toLowerCase()))
                        dictionariesDisplay.add(dictionaries.get(i));
                }
                adapter.notifyDataSetChanged();
            }
        });

        //TODO : ALLER a

    }

    /**
     * Creating the Floating Action Button to add a dictionary through a dialog window
     */
    private void initFloatingActionButton()
    {
        //TODO : animation
        FloatingActionButton addButton = (FloatingActionButton) v.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                create();
            }

        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo  info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        String title = (adapter.getItem(info.position)).getTitle();
        menu.setHeaderTitle(title);

        menu.add(Menu.NONE, CONTEXT_MENU_READ, Menu.NONE, R.string.open);
        menu.add(Menu.NONE, CONTEXT_MENU_UPDATE, Menu.NONE, R.string.rename);
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE, Menu.NONE, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case CONTEXT_MENU_READ:
                read(info.position);
                return true;
            case CONTEXT_MENU_UPDATE:
                update(info.position);
                return true;
            case CONTEXT_MENU_DELETE:
                delete(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    /**
     * Method which allows user to create a dictionary with a unique name
     */
    public void create()
    {
        //Creating the dialog layout
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 0);

        //Creating the EditText to type the dictionary name
        nameBox = new EditText(getActivity());
        nameBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameBox.setHint(R.string.dictionary_name);
        nameBox.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameBox.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        //Adding the EditText to the layout
        layout.addView(nameBox);

        //Creating the dialog builder
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.add_dictionary);

        //Adding the layout to the dialog
        builder.setView(layout);

        //Dialog positive action
        builder.setPositiveButton(R.string.add_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Dictionary d = new Dictionary(nameBox.getText().toString());
                        if (ddm.insert(d) == 1) {
                            dictionariesDisplay.add(d);
                            dictionaries.add(d);
                            searchBox.setText("");
                            read(dictionariesDisplay.indexOf(d));
                        } else {
                            Snackbar.make(rootLayout, R.string.dictionary_not_added, Snackbar.LENGTH_LONG).setAction(R.string.close_button, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                        }
                        dialog.cancel();
                    }
                });

        //Dialog negative action
        builder.setNegativeButton(R.string.cancel_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.setNeutralButton(R.string.csv_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String nameDico = nameBox.getText().toString();
                        if (ddm.select(nameDico) == null){
                            Intent intent = new Intent(HomeFragment.this.getActivity(), CSVImportActivity.class);
                            intent.putExtra(MainActivity.EXTRA_NEW_DICO_NAME, nameDico);
                            startActivity(intent);
                        } else {
                            Snackbar.make(rootLayout, R.string.dico_name_not_available, Snackbar.LENGTH_LONG).setAction(R.string.close_button, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                        }
                    }
                });

        //Creating the dialog and opening the keyboard
        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //Listening the keyboard to handle a "Done" action
        nameBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //Simulating a positive button click. The positive action is executed.
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });

        alertDialog.show();
    }


    /**
     * Method which allows user to open a dictionary. It is Redirecting to the ListWordsActivity.
     *
     * @param position position of the dictionary to read in the dictionariesDisplay list.
     */
    @Override
    public void read(int position)
    {
        Intent intent = new Intent(HomeFragment.this.getActivity(),ListWordsActivity.class);
        if(position != -1)
            intent.putExtra(MainActivity.EXTRA_DICTIONARY, dictionariesDisplay.get(position));
        startActivity(intent);
    }


    /**
     * Method which allows user to rename a dictionary with a unique name.
     *
     * @param position position of the dictionary to update in the dictionariesDisplay list.
     */
    @Override
    public void update(int position)
    {
        final Dictionary d = dictionariesDisplay.get(position);

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 0);

        nameBox = new EditText(getActivity());
        nameBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameBox.setText(d.getTitle());
        nameBox.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameBox.selectAll();
        layout.addView(nameBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.rename_dictionary);
        builder.setView(layout);


        builder.setPositiveButton(R.string.rename_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        final String title = d.getTitle();
                        d.setTitle(nameBox.getText().toString());
                        if (ddm.update(d) == 1) {
                            adapter.notifyDataSetChanged();
                            searchBox.setText("");

                            Snackbar.make(rootLayout, R.string.dictionary_renamed, Snackbar.LENGTH_LONG).setAction(R.string.close_button, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                        } else {
                            d.setTitle(title);
                            Snackbar.make(rootLayout, R.string.dictionary_not_renamed, Snackbar.LENGTH_LONG).setAction(R.string.close_button, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            }).show();
                        }
                        dialog.cancel();
                    }
                });

        builder.setNegativeButton(R.string.cancel_button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        //Creating the dialog and opening the keyboard
        final AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        //Listening the keyboard to handle a "Done" action
        nameBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //Simulating a positive button click. The positive action is executed.
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
        });

        alertDialog.show();

    }

    /**
     * Method which allows user to delete a dictionary.
     *
     * @param position position of the dictionary to delete in the dictionariesDisplay list.
     */
    @Override
    public void delete(final int position)
    {
        final Dictionary d = dictionariesDisplay.get(position);
        dictionariesDisplay.remove(d);
        adapter.notifyDataSetChanged();
        undo = false;
        Snackbar snack = Snackbar.make(rootLayout, d.getTitle() + getString(R.string.deleted), Snackbar.LENGTH_LONG).setAction(R.string.undo_button, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undo = true;
            }
        });
        snack.getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                //Once snackbar is closed, whatever the way : undo button clicked, change activity, an other snackbar, etc.
                if (!undo)
                {
                    dictionaries.remove(d);
                    deleteList.remove(d);
                    ddm.delete(d.getId());
                }
                else
                {
                    dictionariesDisplay.add(position, d);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        snack.show();

    }

    /**
     * Method which allows user to add a dictionary to the deleteList
     *
     * @param position position of the dictionary to add
     */
    @Override
    public void addToDeleteList(int position)
    {
        deleteList.add(dictionariesDisplay.get(position));
        int s = deleteList.size();
        menu.findItem(R.id.nb_item_selected).setTitle(s + " selected");
        menu.findItem(R.id.action_delete_list).setVisible(s>0);
    }

    /**
     * Method which allows user to remove a dictionary to the deleteList
     *
     * @param position position of the dictionary to remove
     */
    @Override
    public void removeFromDeleteList(int position)
    {
        deleteList.remove(dictionariesDisplay.get(position));
        int s = deleteList.size();
        menu.findItem(R.id.nb_item_selected).setTitle(s + " selected");
        menu.findItem(R.id.action_delete_list).setVisible(s > 0);
    }

    @Override
    public void onCreateOptionsMenu(Menu m, MenuInflater inflater)
    {
        menu = m;
        super.onCreateOptionsMenu(menu, inflater);
        showMenu();
    }

    public void showMenu()
    {
        menu.clear();
        if(state == NORMAL_STATE)
            getActivity().getMenuInflater().inflate(R.menu.menu_home, menu);
        else if (state == DELETE_STATE) {
            getActivity().getMenuInflater().inflate(R.menu.menu_home_delete, menu);
            int s = deleteList.size();
            menu.findItem(R.id.nb_item_selected).setTitle(deleteList.size() + " selected");
            menu.findItem(R.id.action_delete_list).setVisible(s > 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.action_add_dictionary:
                create();
                return true;

            case R.id.action_multiple_delete:
                state = DELETE_STATE;
                initGridView();
                showMenu();
                return true;
            case R.id.action_delete_list:
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(getString(R.string.delete) + " " + deleteList.size() + " " + getString(R.string.dictionaries) +  " ?");
                alert.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        for (int i = 0; i < deleteList.size() ; i++)
                        {
                            dictionaries.remove(deleteList.get(i));
                            dictionariesDisplay.remove(deleteList.get(i));
                            ddm.delete(deleteList.get(i).getId());
                        }
                        state = NORMAL_STATE;
                        initGridView();
                        showMenu();
                    }
                });

                alert.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
