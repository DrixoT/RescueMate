package com.rescuemate.data

object MedicalDataConstants {
    fun getMedicalConditions(): List<String> {
        return listOf(
            // Cardiovascular
            "Hypertension (High Blood Pressure)", "Heart Disease", "Coronary Artery Disease", 
            "Heart Failure", "Arrhythmia", "Atrial Fibrillation", "Stroke", "Deep Vein Thrombosis (DVT)",
            "Peripheral Artery Disease", "Aortic Aneurysm", "Congenital Heart Defect",

            // Respiratory
            "Asthma", "COPD", "Chronic Bronchitis", "Emphysema", "Cystic Fibrosis",
            "Pulmonary Hypertension", "Sleep Apnea", "Tuberculosis", "Pneumonia",
            "Sarcoidosis", "Lung Cancer",

            // Endocrine & Metabolic
            "Diabetes Type 1", "Diabetes Type 2", "Gestational Diabetes", "Hypothyroidism",
            "Hyperthyroidism", "Hashimoto's Thyroiditis", "Graves' Disease", "Addison's Disease",
            "Cushing's Syndrome", "Polycystic Ovary Syndrome (PCOS)", "Obesity", "Gout",

            // Neurological
            "Epilepsy", "Seizure Disorder", "Migraine", "Cluster Headaches", "Alzheimer's Disease",
            "Dementia", "Parkinson's Disease", "Multiple Sclerosis", "ALS (Lou Gehrig's)",
            "Huntington's Disease", "Myasthenia Gravis", "Neuropathy", "Cerebral Palsy",
            "Traumatic Brain Injury (TBI)", "Spina Bifida",

            // Mental Health
            "Depression", "Generalized Anxiety Disorder", "Panic Disorder", "Bipolar Disorder",
            "Schizophrenia", "PTSD", "OCD", "ADHD", "Autism Spectrum Disorder",
            "Eating Disorder (Anorexia/Bulimia)", "Substance Use Disorder", "Alcoholism",
            "Borderline Personality Disorder",

            // Immune & Autoimmune
            "Rheumatoid Arthritis", "Lupus (SLE)", "Psoriasis", "Psoriatic Arthritis",
            "Sjogren's Syndrome", "Scleroderma", "Vasculitis", "HIV/AIDS",
            "Primary Immunodeficiency",

            // Gastrointestinal
            "GERD", "Peptic Ulcer Disease", "IBS", "Crohn's Disease", "Ulcerative Colitis",
            "Celiac Disease", "Diverticulitis", "Liver Disease", "Cirrhosis", "Hepatitis B",
            "Hepatitis C", "Fatty Liver Disease", "Pancreatitis", "Gallstones",

            // Kidney & Urinary
            "Chronic Kidney Disease", "Kidney Stones", "Polycystic Kidney Disease",
            "Urinary Incontinence", "Interstitial Cystitis",

            // Musculoskeletal
            "Osteoarthritis", "Osteoporosis", "Fibromyalgia", "Chronic Back Pain",
            "Scoliosis", "Herniated Disc", "Muscular Dystrophy",

            // Blood
            "Anemia", "Sickle Cell Disease", "Thalassemia", "Hemophilia",
            "Deep Vein Thrombosis", "Von Willebrand Disease",

            // Cancer
            "Breast Cancer", "Prostate Cancer", "Lung Cancer", "Colorectal Cancer",
            "Skin Cancer (Melanoma)", "Leukemia", "Lymphoma",

            // Sensory
            "Glaucoma", "Cataracts", "Macular Degeneration", "Hearing Loss", "Tinnitus",
            "Blindness",

            // Other
            "Pregnancy", "Organ Transplant Recipient", "COVID-19 Long Haul",
            "Chronic Fatigue Syndrome", "Endometriosis",
            "None"
        ).sorted()
    }

    fun getCommonMedications(): List<String> {
        return listOf(
            // Pain & Inflammation
            "Aspirin", "Ibuprofen (Advil/Motrin)", "Acetaminophen (Tylenol)", "Naproxen (Aleve)",
            "Celecoxib", "Diclofenac", "Meloxicam", "Indomethacin", "Tramadol",
            "Oxycodone", "Hydrocodone", "Morphine", "Fentanyl", "Codeine",
            "Gabapentin", "Pregabalin (Lyrica)", "Cyclobenzaprine",

            // Cardiovascular
            "Lisinopril", "Losartan", "Amlodipine", "Metoprolol", "Atenolol",
            "Carvedilol", "Clonidine", "Hydrochlorothiazide", "Furosemide (Lasix)",
            "Spironolactone", "Atorvastatin (Lipitor)", "Simvastatin", "Rosuvastatin",
            "Warfarin (Coumadin)", "Apixaban (Eliquis)", "Rivaroxaban (Xarelto)",
            "Clopidogrel (Plavix)", "Nitroglycerin", "Digoxin",

            // Diabetes
            "Metformin", "Insulin Glargine", "Insulin Lispro", "Insulin Aspart",
            "Glipizide", "Sitagliptin", "Jardiance", "Ozempic", "Trulicity",

            // Respiratory
            "Albuterol", "Fluticasone", "Budesonide", "Montelukast (Singulair)",
            "Advair", "Symbicort", "Spiriva", "Prednisone", "Methylprednisolone",

            // Gastrointestinal
            "Omeprazole (Prilosec)", "Pantoprazole", "Esomeprazole (Nexium)",
            "Famotidine (Pepcid)", "Ranitidine", "Ondansetron (Zofran)",
            "Docusate", "Polyethylene Glycol",

            // Mental Health
            "Sertraline (Zoloft)", "Escitalopram (Lexapro)", "Fluoxetine (Prozac)",
            "Citalopram", "Paroxetine", "Duloxetine (Cymbalta)", "Venlafaxine",
            "Bupropion (Wellbutrin)", "Trazodone", "Mirtazapine", "Amitriptyline",
            "Alprazolam (Xanax)", "Lorazepam (Ativan)", "Clonazepam", "Diazepam",
            "Quetiapine", "Aripiprazole", "Risperidone", "Lithium",
            "Adderall", "Ritalin", "Vyvanse",

            // Antibiotics & Antivirals
            "Amoxicillin", "Amoxicillin/Clavulanate", "Azithromycin", "Cephalexin",
            "Ciprofloxacin", "Levofloxacin", "Doxycycline", "Sulfamethoxazole/Trimethoprim",
            "Clindamycin", "Metronidazole", "Nitrofurantoin", "Valacyclovir", "Acyclovir",

            // Other
            "Levothyroxine", "Allopurinol", "Colchicine", "Tamsulosin (Flomax)",
            "Finasteride", "Sildenafil (Viagra)", "Tadalafil (Cialis)",
            "Methotrexate", "Hydroxychloroquine", "Adalimumab (Humira)",
            "Etanercept (Enbrel)", "Infliximab (Remicade)",
            "Oral Contraceptives", "Estradiol", "Testosterone",
            "Vitamin D", "Iron (Ferrous Sulfate)", "Multivitamins", "Fish Oil",
            "None"
        ).sorted()
    }

    fun getCommonAllergies(): List<String> {
        return listOf(
            // Drug Allergies
            "Penicillin", "Amoxicillin", "Ampicillin", "Cephalosporins (Keflex)",
            "Sulfa Drugs (Bactrim)", "Sulfonamides",
            "Aspirin", "Ibuprofen", "NSAIDs",
            "Codeine", "Morphine", "Opioids",
            "Tetracycline", "Erythromycin", "Vancomycin",
            "ACE Inhibitors", "Anticonvulsants",
            "Anesthesia (General)", "Local Anesthetics (Lidocaine)",
            "Contrast Dye (IV)", "Iodine", "Latex",

            // Food Allergies
            "Peanuts", "Tree Nuts (Almonds/Walnuts/Cashews)", "Milk/Dairy", "Eggs",
            "Wheat", "Gluten", "Soy", "Fish", "Shellfish (Shrimp/Crab/Lobster)",
            "Sesame", "Corn", "Tomatoes", "Strawberries", "Citrus Fruits",
            "Chocolate", "Garlic", "Onions", "Mushrooms", "Avocado", "Kiwi", "Bananas",

            // Environmental & Other
            "Pollen", "Grass", "Ragweed", "Trees (Oak/Birch/Maple)",
            "Dust Mites", "Mold", "Cockroaches",
            "Cat Dander", "Dog Dander", "Bird Feathers", "Horse Dander",
            "Bee Stings", "Wasp Stings", "Hornet Stings", "Fire Ant Stings",
            "Mosquito Bites",
            "Nickel", "Gold", "Cobalt", "Chromium",
            "Fragrances", "Perfumes", "Detergents", "Soaps",
            "Preservatives", "Food Dyes (Red 40/Yellow 5)",
            "MSG", "Sulfites", "Adhesives (Band-Aids)", "Sunscreen",
            "No Known Allergies"
        ).sorted()
    }
}
