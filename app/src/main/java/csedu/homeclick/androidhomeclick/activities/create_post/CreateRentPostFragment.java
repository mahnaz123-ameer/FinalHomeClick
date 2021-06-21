package csedu.homeclick.androidhomeclick.activities.create_post;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;


import csedu.homeclick.androidhomeclick.R;
import csedu.homeclick.androidhomeclick.activities.AdFeed;
import csedu.homeclick.androidhomeclick.connector.AdInterface;
import csedu.homeclick.androidhomeclick.connector.AdvertisementService;
import csedu.homeclick.androidhomeclick.connector.UserService;
import csedu.homeclick.androidhomeclick.navigator.ImageRecyclerViewAdapter;
import csedu.homeclick.androidhomeclick.structure.Advertisement;
import csedu.homeclick.androidhomeclick.structure.RentAdvertisement;


public class CreateRentPostFragment extends Fragment implements View.OnClickListener, CalendarView.OnDateChangeListener{
    public static final String TAG = "CreateRentPostFragment";
    private EditText rentAreaName, rentFullAddress, rentBedrooms, rentBathrooms, rentBalconies;
    private EditText rentFloor, rentFloorSpace, rentPayment, rentUtilityCharge, rentDescription;
    private CheckBox rentGas, rentElevator, rentGenerator, rentGarage, rentSecurity;

    private RecyclerView imageRecView;
    private ImageRecyclerViewAdapter imageRecVA = new ImageRecyclerViewAdapter();

    private CalendarView rentAvailableFrom;

    private RadioGroup rentTenant;
    private RadioButton family, single;
    private Button postAd, selectPhotos;


    private UserService userService;
    private final AdvertisementService advertisementService = new AdvertisementService();

    List<Uri> imageUri = new ArrayList<>();
    final ActivityResultLauncher<String> imageSelectorLauncher = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), new ActivityResultCallback<List<Uri>>() {
        @Override
        public void onActivityResult(List<Uri> result) {
            CreateRentPostFragment.this.imagePosition = 0;
            CreateRentPostFragment.this.imageUri.addAll(result);

            //making sure a photo hasn't been added twice to the list
            LinkedHashSet<Uri> hashSet = new LinkedHashSet<>(CreateRentPostFragment.this.imageUri);
            CreateRentPostFragment.this.imageUri = new ArrayList<>(hashSet);

            CreateRentPostFragment.this.imageRecVA = new ImageRecyclerViewAdapter(CreateRentPostFragment.this.getContext(), CreateRentPostFragment.this.imageUri);
            CreateRentPostFragment.this.imageRecView.setAdapter(CreateRentPostFragment.this.imageRecVA);
            CreateRentPostFragment.this.imageRecVA.notifyDataSetChanged();
            LinearLayoutManager llM = new LinearLayoutManager(CreateRentPostFragment.this.getContext());
            llM.setOrientation(LinearLayoutManager.HORIZONTAL);

            CreateRentPostFragment.this.imageRecView.setLayoutManager(llM);

//            if (!CreateRentPostFragment.this.imageUri.isEmpty())
//                Glide.with(CreateRentPostFragment.this).load(imageUri .get(CreateRentPostFragment.this.imagePosition)).into((ImageView) imageView);
        }
    });
    private int imagePosition;
    private final Date[] rentAvailFrom = new Date[1];

    public CreateRentPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_rent_post, container, false);

        bindWidgets(view);
        setClickListeners();

        return view;
    }

    private void setClickListeners() {
        Log.i(TAG, "in set click listeners");
        rentAvailableFrom.setOnDateChangeListener(this::onSelectedDayChange);
        postAd.setOnClickListener(this);

        selectPhotos.setOnClickListener(this);
    }

    private void bindWidgets(View view) {
        Log.i(TAG, "in bind widgets");
        userService = new UserService();


        rentAreaName = view.findViewById(R.id.rentAreaName);
        rentFullAddress = view.findViewById(R.id.rentFullAddress);
        rentBedrooms = view.findViewById(R.id.rentBedrooms);
        rentBathrooms = view.findViewById(R.id.rentBathrooms);
        rentBalconies = view.findViewById(R.id.rentBalconies);
        rentFloor = view.findViewById(R.id.rentFloor);
        rentFloorSpace = view.findViewById(R.id.rentFloorSpace);
        rentGas = view.findViewById(R.id.rentGas);
        rentElevator = view.findViewById(R.id.rentElevator);
        rentGenerator = view.findViewById(R.id.rentGenerator);
        rentGarage = view.findViewById(R.id.rentGarage);
        rentSecurity = view.findViewById(R.id.rentSecurity);
        rentPayment = view.findViewById(R.id.rentPayment);
        rentUtilityCharge = view.findViewById(R.id.rentUtilityCharge);
        rentAvailableFrom = view.findViewById(R.id.rentAvailableFrom);
        rentDescription = view.findViewById(R.id.rentDescription);
        rentTenant = view.findViewById(R.id.rdTenant);
        family = view.findViewById(R.id.rbFamily);
        single = view.findViewById(R.id.rbSinglePerson);
        postAd = view.findViewById(R.id.buttonRentPostAd);
        imageRecView = view.findViewById(R.id.imageRecView);
        selectPhotos = view.findViewById(R.id.select_rent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_rent:
                imageSelectorLauncher.launch("image/*");
                break;

            case R.id.buttonRentPostAd:
                Toast.makeText(getContext().getApplicationContext(), "post ad clicked", Toast.LENGTH_SHORT).show();
                createPost(v);

                break;
            default:
                break;
        }

    }

    private void createPost(View v) {
        Log.i(TAG, "in create post");
        if(checkData()) {
            postAd.setEnabled(false);
            Log.i(TAG, "cleared check data?");
            final RentAdvertisement rentAd = makeAd();

            rentAd.setAdvertiserUID(userService.getUserUID());

            final List<String> fileExtensions = getFileExtensions(imageUri);
            Log.i(TAG, fileExtensions.toString());

            processUploads(fileExtensions, imageUri, rentAd);
        }
    }

    private void processUploads(final List<String> fileExtensions, final List<Uri> uriList, final RentAdvertisement rentAd) {
        Log.i(TAG, "in process uploads");
        advertisementService.getAdId(new AdInterface.OnAdIdListener<Advertisement>() {
            @Override
            public void onAdIdObtained(String adId) {
                rentAd.setAdvertisementID(adId);
                final int[] uploadCount = new int[1];
                uploadCount[0] = 0;
                final List<String> downloadLinks = new ArrayList<>();

                for(int imageCount = 0; imageCount < uriList.size(); imageCount++) {
                    final int total = uriList.size();
                    advertisementService.uploadPhoto(uriList.get(imageCount), fileExtensions.get(imageCount), adId, new AdInterface.OnPhotoUploadListener<String>() {
                        @Override
                        public void ongoingProgress(int percentage) {

                        }

                        @Override
                        public void onCompleteNotify(String downloadUrl) {
                            uploadCount[0]++;
                            downloadLinks.add(downloadUrl);
                            if(uploadCount[0] == total) {
                                rentAd.setUrlToImages(downloadLinks);
                                CreateRentPostFragment.this.completeAdPost(rentAd);
                            }
                        }
                    });
                }

            }
        });
    }

    void completeAdPost(final RentAdvertisement rentAd) {
        advertisementService.completeAdPost(rentAd, new AdInterface.OnAdPostSuccessListener<Boolean>() {
            @Override
            public void OnAdPostSuccessful(Boolean data) {
                Toast.makeText(getContext().getApplicationContext(), "Ad posted successfully.", Toast.LENGTH_SHORT).show();
                CreateRentPostFragment.this.postAd.setEnabled(true);
                CreateRentPostFragment.this.startActivity(new Intent(getContext().getApplicationContext(), AdFeed.class));
            }

            @Override
            public void OnAdPostFailed(String error) {
                Toast.makeText(getContext().getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                CreateRentPostFragment.this.postAd.setEnabled(true);
                CreateRentPostFragment.this.startActivity(new Intent(getContext().getApplicationContext(), AdFeed.class));
            }
        });
    }

    private List<String> getFileExtensions(List<Uri> listUri) {
        List<String> extensions = new ArrayList<>();
        ContentResolver cr = this.getActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        for(Uri uri: listUri) {
            extensions.add(mime.getExtensionFromMimeType(cr.getType(uri)));
        }

        return extensions;
    }

    public RentAdvertisement makeAd() {
        String areaName = rentAreaName.getText().toString().trim();
        String fullAddress = rentFullAddress.getText().toString().trim();
        int numOfBedrooms = Integer.parseInt(rentBedrooms.getText().toString().trim());
        int numOfBathrooms = Integer.parseInt(rentBathrooms.getText().toString().trim());
        int numOfBalconies = Integer.parseInt(rentBalconies.getText().toString().trim());
        int floor = Integer.parseInt(rentFloor.getText().toString().trim());
        int floorSpace = Integer.parseInt(rentFloorSpace.getText().toString().trim());

        Boolean gasAvail = rentGas.isChecked();
        Boolean elevatorAvail = rentElevator.isChecked();
        Boolean generatorAvail = rentGenerator.isChecked();
        Boolean garageAvail = rentGarage.isChecked();
        Boolean securityAvail = rentSecurity.isChecked();

        int payAmount = Integer.parseInt(rentPayment.getText().toString().trim());
        int utilities = Integer.parseInt(rentUtilityCharge.getText().toString().trim());

        String rentDesc = rentDescription.getText().toString().trim();

        String tenantType;
        int checked = rentTenant.getCheckedRadioButtonId();

        if(checked == R.id.rbFamily) {
            tenantType = "Family";
        } else {
            tenantType = "Student/Working Person";
        }

        RentAdvertisement rent = new RentAdvertisement(areaName, fullAddress, "Rent",
                numOfBedrooms, numOfBathrooms, gasAvail, payAmount,
                numOfBalconies, floor, floorSpace, elevatorAvail, generatorAvail,
                garageAvail,  imageUri.size(), tenantType, utilities, rentDesc, securityAvail, rentAvailFrom[0]);

        rent.setAdvertiserUID(userService.getUserUID());

        Log.i("date", rent.getAvailableFrom().toString());
        return rent;
    }

    private Boolean checkData() {
        Log.i(TAG, "in check data");
        Boolean dataOkay = true;
        EditText[] allEditTexts = {rentAreaName, rentFullAddress, rentBedrooms, rentBathrooms, rentBalconies,
                rentFloor, rentFloorSpace, rentPayment, rentUtilityCharge, rentDescription};

        for(EditText e: allEditTexts) {
            if(e.getText().toString().trim().isEmpty()) {
                e.setError("Field cannot be empty");
                dataOkay = false;
            }
        }

        int picked = rentTenant.getCheckedRadioButtonId();

        if(picked != R.id.rbFamily && picked != R.id.rbSinglePerson) {
            Toast.makeText(this.getContext().getApplicationContext(), "You must pick a tenant type.", Toast.LENGTH_SHORT).show();
            dataOkay = false;
        }

        if(this.imageUri.isEmpty()) {
            dataOkay = false;
            Toast.makeText(this.getContext().getApplicationContext(), "You must add photos to your post.", Toast.LENGTH_SHORT).show();
        }

        return dataOkay;
    }

    @Override
    public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        rentAvailFrom[0] = new Date(cal.getTimeInMillis());
        Log.i("date", rentAvailFrom[0].toString());
    }
}