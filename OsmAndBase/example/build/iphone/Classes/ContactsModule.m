/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 * 
 * WARNING: This is generated code. Modify at your own risk and without support.
 */
#ifdef USE_TI_CONTACTS

#import <AddressBookUI/AddressBookUI.h>
#import "ContactsModule.h"
#import "TiContactsPerson.h"
#import "TiContactsGroup.h"
#import "TiApp.h"
#import "TiBase.h"

@implementation ContactsModule

// We'll force the address book to only be accessed on the main thread, for consistency.  Otherwise
// we could run into cross-thread memory issues.
-(ABAddressBookRef)addressBook
{
	if (![NSThread isMainThread]) {
		return NULL;
	}
	
	if (addressBook == NULL) {
		addressBook = ABAddressBookCreate();
	}
	return addressBook;
}

-(void)releaseAddressBook
{
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(releaseAddressBook) withObject:nil waitUntilDone:YES];
		return;
	}
	CFRelease(addressBook);
}

-(void)startup
{
	[super startup];
	addressBook = NULL;
	returnCache = [[NSMutableDictionary alloc] init];
    
    // Force address book creation so that our properties are properly initialized - they aren't
    // defined until the address book is loaded, for some reason.
    [self performSelectorOnMainThread:@selector(addressBook) withObject:nil waitUntilDone:YES];
}

-(void)dealloc
{
	RELEASE_TO_NIL(picker)
	RELEASE_TO_NIL(cancelCallback)
	RELEASE_TO_NIL(selectedPersonCallback)
	RELEASE_TO_NIL(selectedPropertyCallback)
	RELEASE_TO_NIL(returnCache);
	
	[self releaseAddressBook];
	[super dealloc];
}

-(void)removeRecord:(ABRecordRef)record
{
	CFErrorRef error;
	if (!ABAddressBookRemoveRecord([self addressBook], record, &error)) {
		CFStringRef errorStr = CFErrorCopyDescription(error);
		NSString* str = [NSString stringWithString:(NSString*)errorStr];
		CFRelease(errorStr);
		
		NSString* kind = (ABRecordGetRecordType(record) == kABPersonType) ? @"person" : @"group";
		
		[self throwException:[NSString stringWithFormat:@"Failed to remove %@: %@",kind,str]
				   subreason:nil
					location:CODELOCATION];
	}
}

#pragma mark Public API

-(void)save:(id)unused
{
	ENSURE_UI_THREAD(save, unused)
	// ABAddressBookHasUnsavedChanges is broken in pre-3.2
	//if (ABAddressBookHasUnsavedChanges([self addressBook])) {
	CFErrorRef error;
	if (!ABAddressBookSave([self addressBook], &error)) {
		CFStringRef errorStr = CFErrorCopyDescription(error);
		NSString* str = [NSString stringWithString:(NSString*)errorStr];
		CFRelease(errorStr);
		
		[self throwException:[NSString stringWithFormat:@"Unable to save address book: %@",str]
				   subreason:nil
					location:CODELOCATION];
	}
	//}
}

-(void)revert:(id)unused
{
	ENSURE_UI_THREAD(revert, unused)
	// ABAddressBookHasUnsavedChanges is broken in pre-3.2
	//if (ABAddressBookHasUnsavedChanges([self addressBook])) {
	ABAddressBookRevert([self addressBook]);
	//}
}

-(void)showContacts:(id)args
{
	ENSURE_UI_THREAD(showContacts, args);
	ENSURE_SINGLE_ARG(args, NSDictionary)
	
	RELEASE_TO_NIL(cancelCallback)
	RELEASE_TO_NIL(selectedPersonCallback)
	RELEASE_TO_NIL(selectedPropertyCallback)
	RELEASE_TO_NIL(picker)
	
	cancelCallback = [[args objectForKey:@"cancel"] retain];
	selectedPersonCallback = [[args objectForKey:@"selectedPerson"] retain];
	selectedPropertyCallback = [[args objectForKey:@"selectedProperty"] retain];
	
	picker = [[ABPeoplePickerNavigationController alloc] init];
	[picker setPeoplePickerDelegate:self];
	
	animated = [TiUtils boolValue:@"animated" properties:args def:YES];
	
	NSArray* fields = [args objectForKey:@"fields"];
	ENSURE_TYPE_OR_NIL(fields, NSArray)
	
	if (fields != nil) {
		NSMutableArray* pickerFields = [NSMutableArray arrayWithCapacity:[fields count]];
		for (id field in fields) {
			id property = nil;
			if ((property = [[TiContactsPerson contactProperties] objectForKey:field]) ||
				(property = [[TiContactsPerson multiValueProperties] objectForKey:field]))  {
				[pickerFields addObject:property];
			}
		}
		[picker setDisplayedProperties:pickerFields];
	}
	
	[[TiApp app] showModalController:picker animated:animated];
}

// OK to do outside main thread
-(TiContactsPerson*)getPersonByID:(id)arg
{
	ENSURE_SINGLE_ARG(arg,NSNumber)                    
	return [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:[arg intValue] module:self] autorelease];
}

-(TiContactsGroup*)getGroupByID:(id)arg
{
	ENSURE_SINGLE_ARG(arg,NSNumber)
	return [[[TiContactsGroup alloc] _initWithPageContext:[self executionContext] recordId:[arg intValue] module:self] autorelease];
}

-(NSArray*)getPeopleWithName:(id)arg
{
	ENSURE_SINGLE_ARG(arg,NSString)
	
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(getPeopleWithName:) withObject:arg waitUntilDone:YES];
		return [returnCache objectForKey:@"peopleWithName"];
	}
	
	CFArrayRef peopleRefs = ABAddressBookCopyPeopleWithName([self addressBook], (CFStringRef)arg);
	if (peopleRefs == NULL) {
		[returnCache setObject:[NSNull null] forKey:@"peopleWithName"];
		return nil;
	}
	CFIndex count = CFArrayGetCount(peopleRefs);
	NSMutableArray* people = [NSMutableArray arrayWithCapacity:count];
	for (CFIndex i=0; i < count; i++) {
		ABRecordRef ref = CFArrayGetValueAtIndex(peopleRefs, i);
		ABRecordID id_ = ABRecordGetRecordID(ref);
		TiContactsPerson* person = [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
		[people addObject:person];
	}	
	CFRelease(peopleRefs);
	
	[returnCache setObject:people forKey:@"peopleWithName"];
	return people;
}

-(NSArray*)getAllPeople:(id)unused
{
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(getAllPeople:) withObject:unused waitUntilDone:YES];
		return [returnCache objectForKey:@"allPeople"];
	}
	
	CFArrayRef peopleRefs = ABAddressBookCopyArrayOfAllPeople([self addressBook]);
	if (peopleRefs == NULL) {
		[returnCache setObject:[NSNull null] forKey:@"allPeople"];
		return nil;
	}
	CFIndex count = CFArrayGetCount(peopleRefs);
	NSMutableArray* people = [NSMutableArray arrayWithCapacity:count];
	for (CFIndex i=0; i < count; i++) {
		ABRecordRef ref = CFArrayGetValueAtIndex(peopleRefs, i);
		ABRecordID id_ = ABRecordGetRecordID(ref);
		TiContactsPerson* person = [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
		[people addObject:person];
	}	
	CFRelease(peopleRefs);
	
	[returnCache setObject:people forKey:@"allPeople"];
	return people;
}

-(NSArray*)getAllGroups:(id)unused
{
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(getAllGroups:) withObject:unused waitUntilDone:YES];
		return [returnCache objectForKey:@"allGroups"];
	}
	
	CFArrayRef groupRefs = ABAddressBookCopyArrayOfAllGroups([self addressBook]);
	if (groupRefs == NULL) {
		[returnCache setObject:[NSNull null] forKey:@"allGroups"];
		return nil;
	}
	CFIndex count = CFArrayGetCount(groupRefs);
	NSMutableArray* groups = [NSMutableArray arrayWithCapacity:count];
	for (CFIndex i=0; i < count; i++) {
		ABRecordRef ref = CFArrayGetValueAtIndex(groupRefs, i);
		ABRecordID id_ = ABRecordGetRecordID(ref);
		TiContactsGroup* group = [[[TiContactsGroup	alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
		[groups addObject:group];
	}
	CFRelease(groupRefs);
	
	[returnCache setObject:groups forKey:@"allGroups"];
	return groups;
}

-(TiContactsPerson*)createPerson:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary)
    
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(createPerson:) withObject:arg waitUntilDone:YES];
		return [returnCache objectForKey:@"newPerson"];
	}
	
	if (ABAddressBookHasUnsavedChanges([self addressBook])) {
		[self throwException:@"Cannot create a new entry with unsaved changes"
				   subreason:nil
					location:CODELOCATION];
	}
	
	ABRecordRef record = ABPersonCreate();
	[(id)record autorelease];
	CFErrorRef error;
	if (!ABAddressBookAddRecord([self addressBook], record, &error)) {
		CFStringRef errorStr = CFErrorCopyDescription(error);
		NSString* str = [NSString stringWithString:(NSString*)errorStr];
		CFRelease(errorStr);
		
		[self throwException:[NSString stringWithFormat:@"Failed to add person: %@",str]
				   subreason:nil
					location:CODELOCATION];
	}
	[self save:nil];
	
	ABRecordID id_ = ABRecordGetRecordID(record);
	TiContactsPerson* newPerson = [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
	
    [newPerson setValuesForKeysWithDictionary:arg];
    
    if (arg != nil) {
        // Have to save initially so properties can be set; have to save again to commit changes
        [self save:nil];
    }
    
	[returnCache setObject:newPerson forKey:@"newPerson"];
	return newPerson;
}

-(void)removePerson:(id)arg
{
	ENSURE_UI_THREAD(removePerson,arg)
	ENSURE_SINGLE_ARG(arg,TiContactsPerson)
	
	[self removeRecord:[arg record]];
}

-(TiContactsGroup*)createGroup:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary)
    
	if (![NSThread isMainThread]) {
		[self performSelectorOnMainThread:@selector(createGroup:) withObject:arg waitUntilDone:YES];
		return [returnCache objectForKey:@"newGroup"];
	}
	
	if (ABAddressBookHasUnsavedChanges([self addressBook])) {
		[self throwException:@"Cannot create a new entry with unsaved changes"
				   subreason:nil
					location:CODELOCATION];
	}
	
	ABRecordRef record = ABGroupCreate();
	[(id)record autorelease];
	CFErrorRef error;
	if (!ABAddressBookAddRecord([self addressBook], record, &error)) {
		CFStringRef errorStr = CFErrorCopyDescription(error);
		NSString* str = [NSString stringWithString:(NSString*)errorStr];
		CFRelease(errorStr);
		
		[self throwException:[NSString stringWithFormat:@"Failed to add group: %@",str]
				   subreason:nil
					location:CODELOCATION];
	}
	[self save:nil];
	
	ABRecordID id_ = ABRecordGetRecordID(record);
	TiContactsGroup* newGroup = [[[TiContactsGroup alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
	
    [newGroup setValuesForKeysWithDictionary:arg];
    
    if (arg != nil) {
        // Have to save initially so properties can be set; have to save again to commit changes
        [self save:nil];
    }
    
	[returnCache setObject:newGroup forKey:@"newGroup"];
	return newGroup;
}

-(void)removeGroup:(id)arg
{
	ENSURE_UI_THREAD(removePerson,arg)
	ENSURE_SINGLE_ARG(arg,TiContactsGroup)
	
	[self removeRecord:[arg record]];
}

#pragma mark Properties

MAKE_SYSTEM_NUMBER(CONTACTS_KIND_PERSON,[[(NSNumber*)kABPersonKindPerson retain] autorelease])
MAKE_SYSTEM_NUMBER(CONTACTS_KIND_ORGANIZATION,[[(NSNumber*)kABPersonKindOrganization retain] autorelease])

MAKE_SYSTEM_PROP(CONTACTS_SORT_FIRST_NAME,kABPersonSortByFirstName);
MAKE_SYSTEM_PROP(CONTACTS_SORT_LAST_NAME,kABPersonSortByLastName);

#pragma mark Picker delegate functions

-(void)peoplePickerNavigationControllerDidCancel:(ABPeoplePickerNavigationController *)peoplePicker
{
	[[TiApp app] hideModalController:picker animated:animated];
	if (cancelCallback) {
		[self _fireEventToListener:@"cancel" withObject:nil listener:cancelCallback thisObject:nil];
	}
}

-(BOOL)peoplePickerNavigationController:(ABPeoplePickerNavigationController *)peoplePicker shouldContinueAfterSelectingPerson:(ABRecordRef)person
{
	if (selectedPersonCallback) {
		ABRecordID id_ = ABRecordGetRecordID(person);
		TiContactsPerson* person = [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
		[self _fireEventToListener:@"selectedPerson"
						withObject:[NSDictionary dictionaryWithObject:person forKey:@"person"] 
						listener:selectedPersonCallback 
						thisObject:nil];
		[[TiApp app] hideModalController:picker animated:animated];
		return NO;
	}
	return YES;
}

-(BOOL)peoplePickerNavigationController:(ABPeoplePickerNavigationController *)peoplePicker shouldContinueAfterSelectingPerson:(ABRecordRef)person property:(ABPropertyID)property identifier:(ABMultiValueIdentifier)identifier
{
	if (selectedPropertyCallback) {
		ABRecordID id_ = ABRecordGetRecordID(person);
		TiContactsPerson* personObject = [[[TiContactsPerson alloc] _initWithPageContext:[self executionContext] recordId:id_ module:self] autorelease];
		NSString* propertyName = nil;
		id value = nil;
		id label = [NSNull null];
		if (identifier == kABMultiValueInvalidIdentifier) { 
			propertyName = [[[TiContactsPerson contactProperties] allKeysForObject:[NSNumber numberWithInt:property]] objectAtIndex:0];
			CFTypeRef val = ABRecordCopyValue(person, property);
			value = [[(id)val retain] autorelease]; // Force toll-free bridging & autorelease
			CFRelease(val);
		}
		else {
			propertyName = [[[TiContactsPerson multiValueProperties] allKeysForObject:[NSNumber numberWithInt:property]] objectAtIndex:0];
			ABMultiValueRef multival = ABRecordCopyValue(person, property);
			CFIndex index = ABMultiValueGetIndexForIdentifier(multival, identifier);

			CFTypeRef val = ABMultiValueCopyValueAtIndex(multival, index);
			value = [[(id)val retain] autorelease]; // Force toll-free bridging & autorelease
			CFRelease(val);
			
			CFStringRef CFlabel = ABMultiValueCopyLabelAtIndex(multival, index);
			label = [NSString stringWithString:[[[TiContactsPerson multiValueLabels] allKeysForObject:(NSString*)CFlabel] objectAtIndex:0]];
			CFRelease(CFlabel);
			
			CFRelease(multival);
		}
		
		NSDictionary* dict = [NSDictionary dictionaryWithObjectsAndKeys:personObject,@"person",propertyName,@"property",value,@"value",label,@"label",nil];
		[self _fireEventToListener:@"selectedProperty" withObject:dict listener:selectedPropertyCallback thisObject:nil];
		[[TiApp app] hideModalController:picker animated:animated];
		return NO;
	}
	return YES;
}

@end

#endif