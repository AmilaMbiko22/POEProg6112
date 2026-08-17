package com.hospital;

import com.hospital.exception.*;
import com.hospital.model.Inpatient;
import com.hospital.model.Patient;
import com.hospital.model.PatientCategory;
import com.hospital.service.HospitalService;
import com.hospital.ward.Bed;

import java.util.List;
import java.util.Scanner;

/**
 * Patient Admission System. ST10531465 Amila Mbiko DISD0601 Group1
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final HospitalService service = new HospitalService();
    private static final String WARD_NUMBER = "Ward 1";

    public static void main(String[] args) throws DuplicatePatientIdException, PatientNotFoundException,
            InvalidBedException, BedNotAvailableException {
        boolean running = true;
        System.out.println("=================================================");
        System.out.println(" MediCare Hospital - Patient Admission System");
        System.out.println("=================================================");

        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ");
            switch (choice) {
                case 1 -> registerPatient();
                case 2 -> searchPatient();
                case 3 -> updatePatient();
                case 4 -> deletePatient();
                case 5 -> displayAllPatients();
                case 6 -> allocateBed();
                case 7 -> releaseBed();
                case 8 -> service.displayWardLayout();
                case 9 -> displayAvailableBeds();
                case 10 -> displayOccupiedBeds();
                case 11 -> generateWardReport();
                case 12 -> displaySortedPatients();
                case 0 -> {
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> System.out.println("Invalid choice. Please select a valid menu option.");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n----------------- MAIN MENU -----------------");
        System.out.println(" 1. Register a new patient");
        System.out.println(" 2. Search for a patient (by Patient ID)");
        System.out.println(" 3. Update patient details");
        System.out.println(" 4. Delete a patient");
        System.out.println(" 5. Display all registered patients");
        System.out.println(" 6. Allocate a bed to an inpatient");
        System.out.println(" 7. Release a bed (discharge)");
        System.out.println(" 8. Display full ward layout");
        System.out.println(" 9. Display available beds");
        System.out.println("10. Display occupied beds");
        System.out.println("11. Generate ward report");
        System.out.println("12. Display patients sorted by surname/ID");
        System.out.println(" 0. Exit");
        System.out.println("----------------------------------------------");
    }

    // ---------------------------------------------------------------
    // Feature 1: Patient Management
    // ---------------------------------------------------------------

    private static void registerPatient() throws DuplicatePatientIdException {
        System.out.println("\n-- Register New Patient --");
        String id = readNonEmptyString("Patient ID: ");
        String firstName = readNonEmptyString("First Name: ");
        String lastName = readNonEmptyString("Last Name: ");
        int age = readInt("Age: ");
        String gender = readNonEmptyString("Gender: ");
        String condition = readNonEmptyString("Medical Condition: ");
        PatientCategory category = readCategory();

        // Checked with an if statement first, so registerPatient() below
        // is only ever called once we already know it will succeed.
        if (service.findPatientOrNull(id) != null) {
            System.out.println("Error: A patient with ID '" + id + "' is already registered.");
        } else {
            Patient patient;
            if (category == PatientCategory.INPATIENT) {
                patient = new Inpatient(id, firstName, lastName, age, gender, condition, WARD_NUMBER);
            } else {
                patient = new Patient(id, firstName, lastName, age, gender, condition, category);
            }
            service.registerPatient(patient);
            System.out.println("Patient registered successfully.");
        }
    }

    private static void searchPatient() {
        String id = readNonEmptyString("\nEnter Patient ID to search: ");
        Patient patient = service.findPatientOrNull(id);
        if (patient != null) {
            System.out.println(patient.displayDetails());
        } else {
            System.out.println("Error: No patient found with ID '" + id + "'.");
        }
    }

    private static void updatePatient() throws PatientNotFoundException {
        String id = readNonEmptyString("\nEnter Patient ID to update: ");
        Patient existing = service.findPatientOrNull(id);
        if (existing == null) {
            System.out.println("Error: No patient found with ID '" + id + "'.");
        } else {
            System.out.println("Current details: " + existing.displayDetails());
            String firstName = readNonEmptyString("New First Name: ");
            String lastName = readNonEmptyString("New Last Name: ");
            int age = readInt("New Age: ");
            String gender = readNonEmptyString("New Gender: ");
            String condition = readNonEmptyString("New Medical Condition: ");
            service.updatePatient(id, firstName, lastName, age, gender, condition);
            System.out.println("Patient updated successfully.");
        }
    }

    private static void deletePatient() throws PatientNotFoundException {
        String id = readNonEmptyString("\nEnter Patient ID to delete: ");
        if (service.findPatientOrNull(id) == null) {
            System.out.println("Error: No patient found with ID '" + id + "'.");
        } else {
            service.deletePatient(id);
            System.out.println("Patient deleted successfully.");
        }
    }

    private static void displayAllPatients() {
        List<Patient> all = service.getAllPatients();
        System.out.println("\n-- All Registered Patients (" + all.size() + ") --");
        if (all.isEmpty()) {
            System.out.println("No patients registered yet.");
            return;
        }
        for (Patient p : all) {
            System.out.println(p.displayDetails());
        }
    }

    // ---------------------------------------------------------------
    // Feature 2: Bed Management
    // ---------------------------------------------------------------

    private static void allocateBed() throws PatientNotFoundException, BedNotAvailableException {
        String id = readNonEmptyString("\nEnter Patient ID to allocate a bed to: ");
        Patient patient = service.findPatientOrNull(id);

        if (patient == null) {
            System.out.println("Error: No patient found with ID '" + id + "'.");
        } else if (!(patient instanceof Inpatient)) {
            System.out.println("Error: Only Inpatients may be allocated a bed. Patient '" + id
                    + "' is an " + patient.getCategory() + ".");
        } else if (((Inpatient) patient).hasBedAllocated()) {
            System.out.println("Error: Patient '" + id + "' already occupies bed "
                    + ((Inpatient) patient).getBedNumber() + ".");
        } else if (service.getAvailableBeds().isEmpty()) {
            System.out.println("Error: No beds are available in the ward. All 20 beds are occupied.");
        } else {
            String bedNumber = service.allocateBed(id);
            System.out.println("Bed " + bedNumber + " allocated to patient " + id + ".");
        }
    }

    private static void releaseBed() throws PatientNotFoundException, InvalidBedException, BedNotAvailableException {
        String id = readNonEmptyString("\nEnter Patient ID being discharged: ");
        Patient patient = service.findPatientOrNull(id);

        if (patient == null) {
            System.out.println("Error: No patient found with ID '" + id + "'.");
        } else if (!(patient instanceof Inpatient)) {
            System.out.println("Error: Patient '" + id + "' is not an Inpatient and holds no bed.");
        } else if (!((Inpatient) patient).hasBedAllocated()) {
            System.out.println("Error: Patient '" + id + "' does not currently occupy a bed.");
        } else {
            service.releaseBed(id);
            System.out.println("Bed released successfully for patient " + id + ".");
        }
    }

    private static void displayAvailableBeds() {
        List<Bed> beds = service.getAvailableBeds();
        System.out.println("\n-- Available Beds (" + beds.size() + ") --");
        for (Bed b : beds) {
            System.out.println(b.getBedNumber());
        }
    }

    private static void displayOccupiedBeds() {
        List<Bed> beds = service.getOccupiedBeds();
        System.out.println("\n-- Occupied Beds (" + beds.size() + ") --");
        for (Bed b : beds) {
            System.out.println(b.getBedNumber() + " -> Patient ID: " + b.getPatientId());
        }
    }

    // ---------------------------------------------------------------
    // Feature 3: Reports
    // ---------------------------------------------------------------

    private static void generateWardReport() {
        System.out.println("\n================ WARD REPORT ================");
        displayAllPatients();
        displayAvailableBeds();
        displayOccupiedBeds();
        System.out.println("\nTotal registered patients: " + service.getTotalRegisteredPatients());
        System.out.println("Total occupied beds: " + service.getTotalOccupiedBeds());
        System.out.printf("Ward occupancy: %.1f%%%n", service.getWardOccupancyPercentage());
        System.out.println("==============================================");
    }

    private static void displaySortedPatients() {
        System.out.println("\n1. Sort by Surname   2. Sort by Patient ID");
        int choice = readInt("Choice: ");
        List<Patient> sorted = (choice == 2)
                ? service.getPatientsSortedById()
                : service.getPatientsSortedBySurname();
        for (Patient p : sorted) {
            System.out.println(p.displayDetails());
        }
    }

    // ---------------------------------------------------------------
    // Input helpers
    // ---------------------------------------------------------------

    private static PatientCategory readCategory() {
        while (true) {
            System.out.println("Patient Category: 1. Inpatient  2. Outpatient  3. Emergency");
            int choice = readInt("Choice: ");
            switch (choice) {
                case 1: return PatientCategory.INPATIENT;
                case 2: return PatientCategory.OUTPATIENT;
                case 3: return PatientCategory.EMERGENCY;
                default: System.out.println("Invalid choice, please select 1, 2 or 3.");
            }
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (isValidInteger(input)) {
                return parseValidInteger(input);
            }
            System.out.println("Please enter a valid whole number.");
        }
    }

    /**
     * Checks whether the given string is a valid whole number
     */
    private static boolean isValidInteger(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int startIndex = 0;
        if (input.charAt(0) == '-') {
            if (input.length() == 1) {
                return false;
            }
            startIndex = 1;
        }
        for (int i = startIndex; i < input.length(); i++) {
            if (!Character.isDigit(input.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a string already confirmed valid by isValidInteger() into an int.
     */
    private static int parseValidInteger(String input) {
        boolean negative = input.charAt(0) == '-';
        int startIndex = negative ? 1 : 0;
        int value = 0;
        for (int i = startIndex; i < input.length(); i++) {
            value = (value * 10) + Character.getNumericValue(input.charAt(i));
        }
        return negative ? -value : value;
    }

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field cannot be empty.");
        }
    }
}
